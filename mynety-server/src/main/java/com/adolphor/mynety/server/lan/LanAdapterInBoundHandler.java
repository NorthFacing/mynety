package com.adolphor.mynety.server.lan;

import com.adolphor.mynety.common.bean.Address;
import com.adolphor.mynety.common.bean.lan.LanMessage;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.constants.LanMsgType;
import com.adolphor.mynety.common.encryption.CryptFactory;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.utils.BaseUtils;
import com.adolphor.mynety.common.utils.LanMsgUtils;
import com.adolphor.mynety.common.wrapper.AbstractSimpleHandler;
import com.adolphor.mynety.server.config.Config;
import com.adolphor.mynety.server.lan.utils.LanChannelContainers;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

import static com.adolphor.mynety.common.constants.Constants.ATTR_CRYPT_KEY;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_ADDRESS;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_TEMP_MSG;
import static com.adolphor.mynety.common.constants.Constants.COLON;
import static com.adolphor.mynety.common.constants.LanConstants.ATTR_REQUEST_ID;

/**
 * socks server 透传消息给 LAN 处理器，此 Handler 接管 InBoundHandler 的工作
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
@ChannelHandler.Sharable
public final class LanAdapterInBoundHandler extends AbstractSimpleHandler<ByteBuf> {

  public static final LanAdapterInBoundHandler INSTANCE = new LanAdapterInBoundHandler();

  /**
   * 建立 socks 和 Lan 请求之间的绑定关系：
   * 常规InBoundHandler的channelActive方法中中需要建立远程连接，且在 future.isSuccess() 中建立 inRelay 和 outRelay 的相互引用关联。
   * 但在此方法中不一样的地方在于，不需要在future连接建立之后进行关联，而是直接从全局变量中获取 lanChannel 建立连接。
   * <p>
   * 1. 因为所有的inRelayChannel都和lanChannel建立关联，所以需要在这里维护 inRelayChannel和lanChannel的关系：
   * 1.1 将 requestId 添加到当前Channel，这样后续消息来的时候可以从自身属性中获取requestId，用来拼装消息之后发给lan客户端
   * 1.2 将 requestId 以及对应的 inRelayChannel 放到管理容器中
   * 1.3 lan客户端返回消息的时候也会包含requestId信息，这样可以再返回信息的时候，根据这个requestId获取到inRelayChannel，将消息发送给用户
   *
   * @param ctx
   * @throws Exception
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);

    Channel lanChannel = LanChannelContainers.lanChannel;

    if (lanChannel == null || !lanChannel.isActive()) {
      channelClose(ctx);
      return;
    }

    Channel inRelayChannel = ctx.channel();

    String requestId = BaseUtils.getUUID();
    inRelayChannel.attr(ATTR_REQUEST_ID).set(requestId);

    LanChannelContainers.addChannels(requestId, inRelayChannel);

    ICrypt lanCrypt = CryptFactory.get(Config.LAN_METHOD, Config.LAN_PASSWORD);
    LanChannelContainers.requestCryptsMap.put(requestId, lanCrypt);

    Address address = inRelayChannel.attr(ATTR_REQUEST_ADDRESS).get();
    String landDstAddr;
    if (Config.LAN_HOST_NAME.equalsIgnoreCase(address.getHost())) {
      landDstAddr = Constants.LOOPBACK_ADDRESS;
    } else {
      landDstAddr = address.getHost();
    }

    LanMessage lanConnMsg = LanMsgUtils.packageLanMsg(inRelayChannel, requestId, LanMsgType.CONNECT);
    lanConnMsg.setUri(landDstAddr + COLON + address.getPort());
    lanChannel.writeAndFlush(lanConnMsg)
        .addListener((ChannelFutureListener) future -> {
          if (future.isSuccess()) {
            AtomicReference tempMstRef = ctx.channel().attr(ATTR_REQUEST_TEMP_MSG).get();
            if (tempMstRef.get() != null) {
              LanMessage lanMessage = LanMsgUtils.packageLanMsg(inRelayChannel, requestId, LanMsgType.TRANSFER);
              byte[] encryptArray = lanCrypt.encryptToArray((ByteBuf) tempMstRef.get());
              lanMessage.setData(encryptArray);
              lanChannel.writeAndFlush(lanMessage);
            }
          } else {
            logger.warn(ctx.channel().toString(), future.cause());
            channelClose(ctx);
          }
        });
  }

  /**
   * 常规InBoundHandler中，本方法有如下作用：
   * * 本方法的作用，将接收到的消息发送给远程目的地址或存储到缓存列表
   * * 远程目的地址和缓存列表都需要从inRelayChannel中获取。
   * <p>
   * 本handler中，
   * 1. lanChannel已经事先建立连接，所以不需要将信息加入缓存，除非网络连接中断（TODO，网络中断是直接断开连接还是等待响应？）
   * 2. 将接收到的消息通过outRelayChannel也就是lanChannel发送到lan客户端去请求远程目的地址
   * 3. 数据要使用inRelayChannel先解密，再使用outChannel自身的加解密对象进行加密，这样lan客户端才能使用自己对应的加解密对象解密
   *
   * @param ctx
   * @param msg
   * @throws Exception
   */
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    Channel inRelayChannel = ctx.channel();
    Channel lanChannel = LanChannelContainers.lanChannel;
    try {
      String requestId = inRelayChannel.attr(ATTR_REQUEST_ID).get();
      LanMessage lanMessage = LanMsgUtils.packageLanMsg(inRelayChannel, requestId, LanMsgType.TRANSFER);

      ICrypt inRelayCrypt = ctx.channel().attr(ATTR_CRYPT_KEY).get();
      ICrypt lanCrypt = LanChannelContainers.requestCryptsMap.get(requestId);

      ByteBuf decryptBuf = inRelayCrypt.decrypt(msg);
      byte[] encryptArray = lanCrypt.encryptToArray(decryptBuf);

      lanMessage.setData(encryptArray);
      lanChannel.writeAndFlush(lanMessage);
    } catch (Exception e) {
      logger.error("[ " + inRelayChannel.id() + Constants.LOG_MSG_OUT + lanChannel.id() + " ] error", e);
      channelClose(ctx);
    }
  }

}
