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

/**
 * 【握手】处理器：权限验证相关请求
 */
@Slf4j
public class Socks01InitHandler extends SimpleChannelInboundHandler {

  private final ByteBuf buf;

  /**
   * 发送的消息格式：
   * +----+----------+----------+
   * |VER | NMETHODS | METHODS  |
   * +----+----------+----------+
   * | 1  |    1     | 1 to 255 |
   * +----+----------+----------+
   */
  public Socks01InitHandler() {
    buf = Unpooled.buffer(3);
    buf.writeByte(0x05);
    buf.writeByte(0x01);
    buf.writeByte(0x00);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    log.info(Constants.LOG_MSG + ctx.channel() + "【握手】处理器激活，发送初次访问请求：" + ByteBufUtil.hexDump(buf));
    ctx.channel().writeAndFlush(buf);
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
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    ByteBuf buf = (ByteBuf) msg;
    byte ver = buf.readByte();
    byte method = buf.readByte();
    log.info(Constants.LOG_MSG + ctx.channel() + "【握手】处理器收到响应消息：ver={},method={}", ver, method);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.pipeline().remove(this);
    ctx.pipeline().addLast(new Socks03ConnectHandler());
    log.info(Constants.LOG_MSG + ctx.channel() + "【握手】处理器任务完成，添加【连接】处理器");
    ctx.pipeline().fireChannelActive();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
    log.error(Constants.LOG_MSG + ctx.channel() + "【握手】处理器异常：", throwable);
    ctx.channel().close();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    log.info(Constants.LOG_MSG + ctx.channel() + "【握手】处理器连接断开：" + ctx.channel());
    super.channelInactive(ctx);
  }

}
