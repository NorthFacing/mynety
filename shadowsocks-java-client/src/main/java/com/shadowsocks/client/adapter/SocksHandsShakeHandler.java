package com.shadowsocks.client.adapter;

import com.shadowsocks.common.constants.Constants;
import com.shadowsocks.common.nettyWrapper.AbstractSimpleHandler;
import com.shadowsocks.common.utils.SocksServerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;

/**
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public class SocksHandsShakeHandler extends AbstractSimpleHandler<ByteBuf> {

  private final ByteBuf buf;
  private Channel clientChannel;

  /**
   * socks5握手发送的消息格式：
   * +----+----------+----------+
   * |VER | NMETHODS | METHODS  |
   * +----+----------+----------+
   * | 1  |    1     | 1 to 255 |
   * +----+----------+----------+
   */
  public SocksHandsShakeHandler(Channel clientChannel) {
    this.clientChannel = clientChannel;
    buf = Unpooled.buffer(3);
    buf.writeByte(0x05);
    buf.writeByte(0x01);
    buf.writeByte(0x00);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    logger.info("[ {}{}{} ]【socksWrapper】【握手】处理器激活，发送初次访问请求：0x050100", clientChannel, Constants.LOG_MSG_OUT, ctx.channel());
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
    byte ver = msg.readByte();
    byte method = msg.readByte();
    if (ver != 0X05 || method != 0x00) {
      logger.info("[ {}{}{} ]【socksWrapper】【握手】处理器收到响应消息内容错误：ver={},method={}", clientChannel, Constants.LOG_MSG, ctx.channel(), ver, method);
      ctx.close();
    } else {
      logger.info("[ {}{}{} ]【socksWrapper】【握手】处理器收到响应消息：ver={},method={}", clientChannel, Constants.LOG_MSG, ctx.channel(), ver, method);
      ctx.pipeline().addAfter(ctx.name(), null, new SocksConnHandler(clientChannel));
      logger.info("[ {}{}{} ] add handlers: SocksConnHandler", clientChannel, LOG_MSG, ctx.channel());
      ctx.pipeline().remove(this);
      logger.info("[ {}{}{} ] remove handlers: SocksHandsShakeHandler", clientChannel, LOG_MSG, ctx.channel());
      ctx.fireChannelActive();
    }
  }

  @Override
  protected void channelClose(ChannelHandlerContext ctx) {
    SocksServerUtils.flushOnClose(ctx.channel());
  }

}
