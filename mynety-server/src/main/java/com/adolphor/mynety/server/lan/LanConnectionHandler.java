package com.adolphor.mynety.server.lan;

import com.adolphor.mynety.common.bean.lan.LanMessage;
import com.adolphor.mynety.common.wrapper.AbstractSimpleHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.LOG_MSG_IN;
import static com.adolphor.mynety.common.constants.LanConstants.ATTR_LOST_BEAT_CNT;
import static com.adolphor.mynety.common.constants.LanConstants.LAN_MSG_HEARTBEAT;
import static com.adolphor.mynety.common.constants.LanConstants.LAN_MSG_TRANSFER;

/**
 * LAN服务器和客户端连接处理器
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.5
 */
@Slf4j
public class LanConnectionHandler extends AbstractSimpleHandler<LanMessage> {

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    LanChannelContainers.lanChannel = ctx.channel();
    logger.debug("[ {} ] [LanConnectionHandler-channelActive ] active and add to container... ", ctx.channel());
  }

  @Override
  @SuppressWarnings("Duplicates")
  protected void channelRead0(ChannelHandlerContext ctx, LanMessage msg) throws Exception {
    logger.debug("[ {} ] [LanConnectionHandler-channelRead0 ] received response message: {} ", ctx.channel(), msg);
    switch (msg.getType()) {
      case LAN_MSG_HEARTBEAT:
        handleHeartbeatMessage(ctx, msg);
        break;
      case LAN_MSG_TRANSFER:
        handleTransferMessage(ctx, msg);
        break;
      default:
        break;
    }
  }

  /**
   * 数据传输：将数据传输至用户
   *
   * @param ctx
   * @param msg 数据请求信息
   */
  private void handleTransferMessage(ChannelHandlerContext ctx, LanMessage msg) {
    ByteBuf buf = ctx.alloc().buffer(msg.getData().length);
    buf.writeBytes(msg.getData());
    String requestId = msg.getRequestId();
    Channel requetsChannel = LanChannelContainers.getChannelById(requestId);
    requetsChannel.writeAndFlush(buf);
    logger.debug("[ {}{}{} ] [LanConnectionHandler-channelRead0 ] handleTransferMessage response message, write to requestChannel: {} bytes", requetsChannel, LOG_MSG_IN, ctx.channel(), buf.readableBytes());
  }

  /**
   * 心跳处理，复原超时计数，并直接返回心跳信息
   *
   * @param ctx
   * @param lanMessage 心跳信息
   */
  private void handleHeartbeatMessage(ChannelHandlerContext ctx, LanMessage lanMessage) {
    logger.debug("[ {} ] [LanConnectionHandler-channelRead0 ] handleHeartbeatMessage response heartbeat message: {}", ctx.channel(), lanMessage);
    ctx.channel().attr(ATTR_LOST_BEAT_CNT).set(0L);
    ctx.channel().writeAndFlush(lanMessage);
  }

}