package com.adolphor.mynety.server.lan;

import com.adolphor.mynety.common.bean.lan.LanMessage;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.utils.ByteStrUtils;
import com.adolphor.mynety.common.utils.ChannelUtils;
import com.adolphor.mynety.common.wrapper.AbstractSimpleHandler;
import com.adolphor.mynety.server.lan.utils.LanChannelContainers;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

import static com.adolphor.mynety.common.constants.Constants.ATTR_CONNECTED_TIMESTAMP;
import static com.adolphor.mynety.common.constants.Constants.ATTR_CRYPT_KEY;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG_IN;
import static com.adolphor.mynety.common.constants.LanConstants.ATTR_LOST_BEAT_CNT;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * lan服务器，监听lan客户端的请求。虽然此handler是一个监听服务端，但承担的是常规情况下的outBoundHandler的角色，
 * 所以收到消息之后，需要将消息返回给inRelayChannel，而inRelayChannel的获取条件是消息中的requestId。
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
@ChannelHandler.Sharable
public class LanOutBoundHandler extends AbstractSimpleHandler<LanMessage> {

  public static final LanOutBoundHandler INSTANCE = new LanOutBoundHandler();

  /**
   * 当有 lan client 请求的过来的时候，建立和client的连接，并将此连接保存到全局变量
   *
   * @param ctx
   * @throws Exception
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
    LanChannelContainers.lanChannel = ctx.channel();

    logger.debug("[ {} ]【{}】与lan客户端的连接创建成功... ", ctx.channel(), getSimpleName(this));
  }

  /**
   * 收到lan客户端发送过来的消息：
   * 1. 心跳信息：心跳丢失信息重置，并返回客户端心跳确认信息
   * 2. 请求回复信息：将信息加密之后返回给socks server中的inRelayChannel
   *
   * @param ctx
   * @param msg
   * @throws Exception
   */
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, LanMessage msg) throws Exception {
    switch (msg.getType()) {
      case HEARTBEAT:
        handleHeartbeatMessage(ctx, msg);
        break;
      case TRANSFER:
        handleTransferMessage(ctx, msg);
        break;
      case DISCONNECT:
        handleDisconnectMessage(ctx, msg);
        break;
      default:
        break;
    }
  }

  /**
   * 断开连接：lan客户端和目的连接断开的时候，会发送断开连接信息，然后这里会断开socks服务器和客户端的连接
   *
   * @param ctx
   * @param msg
   */
  private void handleDisconnectMessage(ChannelHandlerContext ctx, LanMessage msg) {
    Channel inRelayChannel = LanChannelContainers.getChannelByRequestId(msg.getRequestId());
    long connTime = System.currentTimeMillis() - ctx.channel().attr(ATTR_CONNECTED_TIMESTAMP).get();
    logger.info("[ {} ]inRelayChannel will be closed by requestId, connection time: {}ms", inRelayChannel, getSimpleName(this), connTime);
    ChannelUtils.closeOnFlush(inRelayChannel);
  }

  /**
   * 数据传输：将LAN客户端回复数据返回给对应的inRelayChannel
   * 1. 先用 lanChannel 对应的加解密对象解密
   * 2. 在使用 inRelayChannel 对应的加解密对象加密
   *
   * @param ctx
   * @param msg 回复信息
   */
  private void handleTransferMessage(ChannelHandlerContext ctx, LanMessage msg) throws Exception {
    String requestId = msg.getRequestId();
    Channel inRelayChannel = LanChannelContainers.getChannelByRequestId(requestId);
    logger.debug("[ {}{}{} ] {} receive reply msg: {}", inRelayChannel.id(), LOG_MSG_IN, ctx.channel().id(), getSimpleName(this), msg);
    byte[] data = msg.getData();

    ICrypt inRelayCrypt = inRelayChannel.attr(ATTR_CRYPT_KEY).get();
    ICrypt lanCrypt = LanChannelContainers.requestCryptsMap.get(requestId);

    ByteBuf decryptBuf = lanCrypt.decrypt(ByteStrUtils.getHeapBuf(data));
    ByteBuf encryptBuf = inRelayCrypt.encrypt(decryptBuf);

    inRelayChannel.writeAndFlush(encryptBuf);
    logger.debug("[ {}{}{} ] {} write reply msg to socks server: {} bytes", inRelayChannel.id(), LOG_MSG_IN, ctx.channel().id(), getSimpleName(this), encryptBuf.readableBytes());
  }

  /**
   * 心跳处理，复原超时计数，并直接返回心跳信息
   *
   * @param ctx
   * @param msg 心跳信息
   */
  private void handleHeartbeatMessage(ChannelHandlerContext ctx, LanMessage msg) {
    ctx.channel().attr(ATTR_LOST_BEAT_CNT).set(0L);
    ctx.channel().writeAndFlush(msg);
  }

  /**
   * TODO need to refactor, if lost lan connection, waits for 3 heart beats time
   */
  @Override
  public void channelClose(ChannelHandlerContext ctx) {
    Collection<Channel> allChannels = LanChannelContainers.getAllChannels();
    for (Channel ch : allChannels) {
      ChannelUtils.closeOnFlush(ch);
    }
    super.channelClose(ctx);
  }


}