package com.adolphor.mynety.server.lan;

import com.adolphor.mynety.common.bean.lan.LanMessage;
import com.adolphor.mynety.common.encryption.CryptUtil;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.utils.ByteStrUtils;
import com.adolphor.mynety.common.utils.ChannelUtils;
import com.adolphor.mynety.common.wrapper.AbstractSimpleHandler;
import com.adolphor.mynety.server.lan.utils.LanChannelContainers;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

import static com.adolphor.mynety.common.constants.Constants.ATTR_CONNECTED_TIMESTAMP;
import static com.adolphor.mynety.common.constants.Constants.ATTR_CRYPT_KEY;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG_IN;
import static com.adolphor.mynety.common.constants.LanConstants.ATTR_LOST_BEAT_CNT;
import static com.adolphor.mynety.common.constants.LanConstants.LAN_MSG_DISCONNECT;
import static com.adolphor.mynety.common.constants.LanConstants.LAN_MSG_HEARTBEAT;
import static com.adolphor.mynety.common.constants.LanConstants.LAN_MSG_TRANSFER;
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
public class LanConnInBoundHandler extends AbstractSimpleHandler<LanMessage> {

  public static final LanConnInBoundHandler INSTANCE = new LanConnInBoundHandler();

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
    logger.debug("[ {} ]【{}】收到lan客户端的消息: {} ", ctx.channel().id(), getSimpleName(this), msg);
    switch (msg.getType()) {
      case LAN_MSG_HEARTBEAT:
        handleHeartbeatMessage(ctx, msg);
        break;
      case LAN_MSG_TRANSFER:
        handleTransferMessage(ctx, msg);
        break;
      case LAN_MSG_DISCONNECT:
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
    logger.info("[ {} ]【{}】根据requestId关闭连接，共计连接时间: {}ms", inRelayChannel, getSimpleName(this), connTime);
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
  private void handleTransferMessage(ChannelHandlerContext ctx, LanMessage msg) {
    String requestId = msg.getRequestId();
    Channel inRelayChannel = LanChannelContainers.getChannelByRequestId(requestId);
    logger.debug("[ {}{}{} ]【{}】收到回复信息: {}", inRelayChannel.id(), LOG_MSG_IN, ctx.channel().id(), getSimpleName(this), msg);
    byte[] data = msg.getData();
    logger.debug("[ {}{}{} ]【{}】收到回复信息-加密的data内容: {} bytes => {}", inRelayChannel.id(), LOG_MSG_IN, ctx.channel().id(), getSimpleName(this), data.length, data);

    ICrypt inRelayCrypt = inRelayChannel.attr(ATTR_CRYPT_KEY).get();
    ICrypt lanCrypt = LanChannelContainers.requestCryptsMap.get(requestId);

    byte[] decrypt = CryptUtil.decrypt(lanCrypt, ByteStrUtils.getDirectByteBuf(data));
    byte[] encrypt = CryptUtil.encrypt(inRelayCrypt, ByteStrUtils.getDirectByteBuf(decrypt));

    inRelayChannel.writeAndFlush(Unpooled.directBuffer().writeBytes(encrypt));
    logger.debug("[ {}{}{} ]【{}】将回复信息返回给socks服务器: {} bytes => {}", inRelayChannel.id(), LOG_MSG_IN, ctx.channel().id(), data.length, data);
  }

  /**
   * 心跳处理，复原超时计数，并直接返回心跳信息
   *
   * @param ctx
   * @param msg 心跳信息
   */
  private void handleHeartbeatMessage(ChannelHandlerContext ctx, LanMessage msg) {
    logger.debug("[ {} ]【{}】处理心跳信息: {}", ctx.channel().id(), getSimpleName(this), msg);
    ctx.channel().attr(ATTR_LOST_BEAT_CNT).set(0L);
    ctx.channel().writeAndFlush(msg);
  }

  /**
   * 如果客户端断开连接，那么server和client之间的所有连接都要断开
   */
  @Override
  public void channelClose(ChannelHandlerContext ctx) {
    // 先关闭服务端和客户端的所有连接
    Collection<Channel> allChannels = LanChannelContainers.getAllChannels();
    for (Channel ch : allChannels) {
      long connTime = System.currentTimeMillis() - ctx.channel().attr(ATTR_CONNECTED_TIMESTAMP).get();
      logger.info("[ {} ]【{}】关闭连接，共计连接时间: {}ms", ctx.channel().id(), getSimpleName(this), connTime);
      ChannelUtils.closeOnFlush(ch);
    }
    // 再关闭lan服务端和客户端的链接
    long connTime = System.currentTimeMillis() - ctx.channel().attr(ATTR_CONNECTED_TIMESTAMP).get();
    logger.info("[ {} ]【{}】关闭连接，共计连接时间: {}ms", ctx.channel().id(), getSimpleName(this), connTime);
    ChannelUtils.closeOnFlush(ctx.channel());

  }


}