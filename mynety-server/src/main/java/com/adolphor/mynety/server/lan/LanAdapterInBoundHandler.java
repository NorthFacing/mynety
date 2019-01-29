package com.adolphor.mynety.server.lan;

import com.adolphor.mynety.common.bean.Address;
import com.adolphor.mynety.common.bean.lan.LanMessage;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.encryption.CryptFactory;
import com.adolphor.mynety.common.encryption.CryptUtil;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.utils.ByteStrUtils;
import com.adolphor.mynety.common.utils.ChannelUtils;
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
import static com.adolphor.mynety.common.constants.LanConstants.ATTR_REQUEST_ID;
import static com.adolphor.mynety.common.constants.LanConstants.LAN_MSG_CONNECT;
import static com.adolphor.mynety.common.constants.LanConstants.LAN_MSG_TRANSFER;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

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

    Channel inRelayChannel = ctx.channel();
    Channel lanChannel = LanChannelContainers.lanChannel;

    logger.debug("[ {}{}{} ]【{}】准备建立socks和Lan之间的连接...", inRelayChannel.id(), Constants.LOG_MSG, lanChannel != null ? lanChannel.id() : "", getSimpleName(this));

    String requestId = ChannelUtils.getUUID();
    LanChannelContainers.addChannels(requestId, inRelayChannel);
    inRelayChannel.attr(ATTR_REQUEST_ID).set(requestId);

    // lan 服务端和 lan 客户端之间的通信，要使用各自的channel对应的加密对象
    ICrypt lanCrypt = CryptFactory.get(Config.PROXY_METHOD, Config.PROXY_PASSWORD);
    LanChannelContainers.requestCryptsMap.put(requestId, lanCrypt);

    Address address = inRelayChannel.attr(ATTR_REQUEST_ADDRESS).get();
    String dstAddr = address.getHost();
    Integer dstPort = address.getPort();

    if (lanChannel == null || !lanChannel.isActive()) {
      logger.error("[ {}{}{} ]【{}】lanChannel 失效...", inRelayChannel.id(), Constants.LOG_MSG, lanChannel != null ? lanChannel.id() : "", getSimpleName(this));
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

    // 第一次请求，发送带有地址的请求，使Lan客户端和目的地建立连接
    LanMessage lanConnMsg = LanMsgUtils.packageLanMsg(inRelayChannel, requestId, LAN_MSG_CONNECT);
    lanConnMsg.setUri(landDstAddr + ":" + dstPort);
    logger.debug("[ {}{}{} ]【{}】发送给Lan客户端的第一条信息 => 建立连接: {}", inRelayChannel.id(), Constants.LOG_MSG_OUT, lanChannel.id(), getSimpleName(this), lanConnMsg);
    lanChannel.writeAndFlush(lanConnMsg).addListener((ChannelFutureListener) future -> {
      // 第二次请求，缓存的请求信息
      AtomicReference tempMstRef = ctx.channel().attr(ATTR_REQUEST_TEMP_MSG).get();
      if (tempMstRef != null && tempMstRef.get() != null) {
        LanMessage lanMessage = LanMsgUtils.packageLanMsg(inRelayChannel, requestId, LAN_MSG_TRANSFER);
        // 发送给lan客户端的加密使用自己channel对应的加密对象
        lanMessage.setData(CryptUtil.encrypt(lanCrypt, tempMstRef.get()));
        lanChannel.writeAndFlush(lanMessage);
        logger.debug("[ {}{}{} ]【{}】发送给Lan客户端的第二条信息 => 缓存信息: {}", inRelayChannel.id(), Constants.LOG_MSG_OUT, lanChannel.id(), getSimpleName(this), lanMessage);
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
    logger.debug("[ {}{}{} ]【{}】接收到请求信息，将要发送给lan客户端: {}", inRelayChannel.id(), Constants.LOG_MSG_OUT, lanChannel.id(), getSimpleName(this), ByteStrUtils.getByteArr(msg.copy()));
    try {
      String requestId = inRelayChannel.attr(ATTR_REQUEST_ID).get();
      LanMessage lanMessage = LanMsgUtils.packageLanMsg(inRelayChannel, requestId, LAN_MSG_TRANSFER);

      ICrypt inRelayCrypt = ctx.channel().attr(ATTR_CRYPT_KEY).get();
      ICrypt lanCrypt = LanChannelContainers.requestCryptsMap.get(requestId);

      byte[] decrypt = CryptUtil.decrypt(inRelayCrypt, msg);
      byte[] encrypt = CryptUtil.encrypt(lanCrypt, ByteStrUtils.getDirectByteBuf(decrypt));

      lanMessage.setData(encrypt);
      logger.debug("[ {}{}{} ]【{}】接收到请求信息，发送给lan的请求信息: {}", inRelayChannel.id(), Constants.LOG_MSG_OUT, lanChannel.id(), getSimpleName(this), lanMessage);
      lanChannel.writeAndFlush(lanMessage);
    } catch (Exception e) {
      logger.error("[ " + inRelayChannel.id() + Constants.LOG_MSG_OUT + lanChannel.id() + " ] error", e);
      channelClose(ctx);
    }
  }

}
