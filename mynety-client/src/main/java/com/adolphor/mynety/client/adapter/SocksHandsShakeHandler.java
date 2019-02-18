package com.adolphor.mynety.client.adapter;

import com.adolphor.mynety.common.wrapper.AbstractSimpleHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;

import static com.adolphor.mynety.common.constants.Constants.RESERVED_BYTE;
import static com.adolphor.mynety.common.constants.HandlerName.socksConnHandler;
import static com.adolphor.mynety.common.constants.HandlerName.socksShakerHandler;

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
   * socks5 handshake msg format:
   * +----+----------+----------+
   * |VER | NMETHODS | METHODS  |
   * +----+----------+----------+
   * | 1  |    1     | 1 to 255 |
   * +----+----------+----------+
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
    ReferenceCountUtil.retain(buf);
    ctx.writeAndFlush(buf);
  }

  /**
   * return msg format:
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
    if (ver != SocksVersion.SOCKS5.byteValue() || method != RESERVED_BYTE) {
      throw new ConnectException("do NOT sport socks5!");
    }
    ctx.pipeline().addAfter(socksShakerHandler,socksConnHandler, SocksConnHandler.INSTANCE);
    ctx.pipeline().remove(socksShakerHandler);
    ctx.fireChannelActive();
  }

}
