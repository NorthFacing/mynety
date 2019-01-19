package com.adolphor.mynety.client.adapter;

import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.wrapper.AbstractSimpleHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.ATTR_IN_RELAY_CHANNEL;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG;
import static com.adolphor.mynety.common.constants.Constants.RESERVED_BYTE;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.4
 */
@Slf4j
@ChannelHandler.Sharable
public class SocksHandsShakeHandler extends AbstractSimpleHandler<ByteBuf> {

  public static final SocksHandsShakeHandler INSTANCE = new SocksHandsShakeHandler();

  private static final ByteBuf buf = Unpooled.buffer(3);

  static {
    buf.writeByte(SocksVersion.SOCKS5.byteValue());
    buf.writeByte(0x01);
    buf.writeByte(RESERVED_BYTE);
  }


  /**
   * socks5握手发送的消息格式：
   * +----+----------+----------+
   * |VER | NMETHODS | METHODS  |
   * +----+----------+----------+
   * | 1  |    1     | 1 to 255 |
   * +----+----------+----------+
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);

    Channel inRelayChannel = ctx.channel().attr(ATTR_IN_RELAY_CHANNEL).get();
    logger.info("[ {}{}{} ]【握手】发送初次访问请求：0x050100", inRelayChannel.id(), Constants.LOG_MSG_OUT, ctx.channel().id());
    ReferenceCountUtil.retain(buf);
    ctx.writeAndFlush(buf);
  }

  /**
   * 接收到的消息格式：
   * +----+----------+
   * |VER |  METHOD  |
   * +----+----------+
   * | 1  |    1     |
   * +----+----------+
   *
   * @param ctx
   * @param msg
   * @throws Exception
   */
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    Channel inRelayChannel = ctx.channel().attr(ATTR_IN_RELAY_CHANNEL).get();
    logger.info("[ {}{}{} ]【握手】处理器收到响应消息：{} bytes = {}", inRelayChannel.id(), Constants.LOG_MSG, ctx.channel().id(), msg.readableBytes(), msg);
    byte ver = msg.readByte();
    byte method = msg.readByte();
    if (ver != SocksVersion.SOCKS5.byteValue() || method != RESERVED_BYTE) {
      logger.info("[ {}{}{} ]【握手】处理器收到响应消息内容错误：ver={},method={}", inRelayChannel.id(), Constants.LOG_MSG, ctx.channel().id(), ver, method);
      ctx.close();
    } else {
      logger.info("[ {}{}{} ]【握手】处理器收到响应消息：ver={},method={}", inRelayChannel.id(), Constants.LOG_MSG, ctx.channel().id(), ver, method);
      ctx.pipeline().addLast(SocksConnHandler.INSTANCE);
      logger.info("[ {}{}{} ]【握手】添加处理器：SocksConnHandler", inRelayChannel.id(), LOG_MSG, ctx.channel().id());
      ctx.pipeline().remove(this);
      logger.info("[ {}{}{} ]【握手】移除处理器: SocksHandsShakeHandler", inRelayChannel.id(), LOG_MSG, ctx.channel().id());
      ctx.fireChannelActive();
    }
  }

}
