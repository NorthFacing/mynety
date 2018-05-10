package com.adolphor.mynety.server.lan;

import com.adolphor.mynety.common.bean.Address;
import com.adolphor.mynety.common.bean.lan.LanMessage;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.wrapper.AbstractSimpleHandler;
import com.adolphor.mynety.common.utils.ByteStrUtils;
import com.adolphor.mynety.common.utils.SocksServerUtils;
import com.adolphor.mynety.server.config.Config;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_ADDRESS;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_TEMP_LIST;
import static com.adolphor.mynety.common.constants.LanConstants.ATTR_REQUEST_ID;
import static com.adolphor.mynety.common.constants.LanConstants.LAN_MSG_TRANSFER;

/**
 * socks server 透传 LAN 处理器
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Slf4j
public final class LanAdapterHandler extends AbstractSimpleHandler<ByteBuf> {

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    Channel requestChannel = ctx.channel();
    Channel lanChannel = LanChannelContainers.lanChannel;

    logger.debug("[ {}{}{} ] [LanAdapterHandler-channelActive] socks server to lan channelActive...", requestChannel, Constants.LOG_MSG);

    // 将 目标地址 对应的channel 绑定上 requestId：
    // 1. server发送消息时应该包含该字段，这样LAN client 能够返回消息的时候附带上此字段
    // 2. server接收到后续消息（后续消息不包含URI地址）的时候，根据requestId找到对应的 requestChannel
    // 3. 将requestId加入requestChannel的attr属性，这样后续的request请求可以获取到此requestId字段
    String requestId = SocksServerUtils.getUUID();
    LanChannelContainers.addChannels(requestId, requestChannel);
    requestChannel.attr(ATTR_REQUEST_ID).set(requestId);

    Address address = requestChannel.attr(ATTR_REQUEST_ADDRESS).get();
    String dstAddr = address.getHost();
    Integer dstPort = address.getPort();

    if (lanChannel == null || !lanChannel.isActive()) {
      logger.error("[ {}{}{} ] [LanAdapterHandler-channelActive] lanChannel is NOT active...", requestChannel, Constants.LOG_MSG, lanChannel);
      channelClose(ctx);
      return;
    }

    // 如果是设置的LAN域名，则转发地址应该是LAN服务器本身(127.0.0.1)
    String landDstAddr;
    if (Config.LAN_HOST_NAME.equalsIgnoreCase(dstAddr)) {
      landDstAddr = Constants.LOOPBACK_ADDRESS;
    } else {
      landDstAddr = dstAddr;
    }

    // 第一次请求，发送带有地址的请求，建立连接
    Long incredSerNo = LanMessage.getIncredSerNo(requestChannel);
    LanMessage lanConnMsg = new LanMessage();
    lanConnMsg.setType(LAN_MSG_TRANSFER);
    lanConnMsg.setSerialNumber(incredSerNo);
    lanConnMsg.setRequestId(requestId);
    lanConnMsg.setUri(landDstAddr + ":" + dstPort);
    logger.debug("[ {}{}{} ] [LanAdapterHandler-channelActive] connect to lan client: {}", requestChannel, Constants.LOG_MSG_OUT, lanChannel, lanConnMsg);
    lanChannel.writeAndFlush(lanConnMsg);

    synchronized (this) {
      List<ByteBuf> list = requestChannel.attr(ATTR_REQUEST_TEMP_LIST).get();
      if (list != null && list.size() > 0) {
        list.forEach(request -> {
          if (request.readableBytes() <= 0) {
            return;
          }
          Long incSerNo = LanMessage.getIncredSerNo(requestChannel);
          LanMessage lanMessage = new LanMessage();
          lanMessage.setType(LAN_MSG_TRANSFER);
          lanMessage.setSerialNumber(incSerNo);
          lanMessage.setRequestId(requestId);
          lanMessage.setData(ByteStrUtils.getByteArr(request));
          logger.debug("[ {}{}{} ] [LanAdapterHandler-channelActive] write temp msg to lan client: {}", requestChannel, Constants.LOG_MSG_OUT, lanChannel, lanMessage);
          lanChannel.writeAndFlush(lanMessage);
        });
      }
      list.clear();
    }
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    Channel requestChannel = ctx.channel();
    Channel lanChannel = LanChannelContainers.lanChannel;

    logger.debug("[ {}{}{} ] [LanAdapterHandler-channelRead0] lan adapter received msg: {} bytes => {}", requestChannel, Constants.LOG_MSG, lanChannel, msg.readableBytes(), msg);

    try {

      String reqeustId = requestChannel.attr(ATTR_REQUEST_ID).get();
      Long incredSerNo = LanMessage.getIncredSerNo(requestChannel);

      LanMessage lanMessage = new LanMessage();
      lanMessage.setType(LAN_MSG_TRANSFER);
      lanMessage.setSerialNumber(incredSerNo);
      lanMessage.setRequestId(reqeustId);
      lanMessage.setData(ByteStrUtils.getByteArr(msg));

      logger.debug("[ {}{}{} ] [LanAdapterHandler-channelRead0] write to lan client: {}", requestChannel, Constants.LOG_MSG_OUT, lanChannel, lanMessage);
      lanChannel.writeAndFlush(lanMessage);
    } catch (Exception e) {
      logger.error("[ " + requestChannel + Constants.LOG_MSG_OUT + lanChannel + " ] error", e);
      channelClose(ctx);
    }
  }

}
