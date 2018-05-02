/**
 * MIT License
 * <p>
 * Copyright (c) 2018 0haizhu0@gmail.com
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.shadowsocks.client.socks;

import com.shadowsocks.common.constants.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import static com.shadowsocks.client.socks.SocksClientMainTest.DST_HOST;

@Slf4j
public class Socks03ConnectHandler extends SimpleChannelInboundHandler {

  private final ByteBuf buf;

  /**
   * 发送的消息格式：
   * +----+-----+-------+------+----------+----------+
   * |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.ATTR_PORT |
   * +----+-----+-------+------+----------+----------+
   * | 1  |  1  | X'00' |  1   | Variable |    2     |
   * +----+-----+-------+------+----------+----------+
   */
  public Socks03ConnectHandler() {
    buf = Unpooled.buffer();
    buf.writeByte(0x05);
    buf.writeByte(0x01);
    buf.writeByte(0x00);
    buf.writeByte(0x03); // domain
    byte[] bytes = DST_HOST.getBytes();
    buf.writeByte(bytes.length); // ADDR.LEN
    buf.writeBytes(bytes); // ADDR.LEN
    buf.writeShort(443); // port
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    logger.info(Constants.LOG_MSG + ctx.channel() + "【连接】处理器激活，发送网页请求：" + ByteBufUtil.hexDump(buf));
    ctx.channel().writeAndFlush(buf);
  }

  /**
   * +----+-----+-------+------+----------+----------+
   * |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.ATTR_PORT |
   * +----+-----+-------+------+----------+----------+
   * | 1  |  1  | X'00' |  1   | Variable |    2     |
   * +----+-----+-------+------+----------+----------+
   *
   * @param ctx
   * @param msg
   * @throws Exception
   */
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    ByteBuf result = (ByteBuf) msg;
    byte ver = result.readByte();
    byte cmd = result.readByte();
    byte psv = result.readByte();
    byte atyp = result.readByte();

    byte dstLen = result.readByte();
    ByteBuf addrBuf = result.readBytes(dstLen);
    String addr = ByteBufUtil.hexDump(addrBuf);
    short port = result.readShort();
    logger.info(Constants.LOG_MSG + ctx.channel() + "【连接】处理器收到响应消息：ver={}, cmd={}, psv={}, atyp={}, dstLen={}, addr={}, port={}",
        ver, cmd, psv, atyp, dstLen, addr, port);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.pipeline().remove(this);
    ctx.pipeline().addLast(new Socks04DataHandler());
    logger.info(Constants.LOG_MSG + ctx.channel() + "【连接】处理器任务完成，添加【数据】处理器");
    ctx.pipeline().fireChannelActive();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
    logger.error(Constants.LOG_MSG + ctx.channel() + "【连接】处理器异常：", throwable);
    ctx.channel().close();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    logger.info(Constants.LOG_MSG + ctx.channel() + "【连接】处理器连接断开：" + ctx.channel());
    super.channelInactive(ctx);
  }

}
