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
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;

import static com.shadowsocks.client.socks.SocksClientMainTest.DST_HOST;
import static com.shadowsocks.client.socks.SocksClientMainTest.DST_PORT;
import static com.shadowsocks.client.socks.SocksClientMainTest.DST_PROTOCOL;

@Slf4j
public class Socks04DataHandler extends SimpleChannelInboundHandler {

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.info(Constants.LOG_MSG + ctx.channel() + "【数据】处理器激活，发送真实请求...");
    ctx.channel().writeAndFlush(Unpooled.wrappedBuffer(getHttpRequest().getBytes("UTF-8")));
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    logger.info(Constants.LOG_MSG + ctx.channel() + "【数据】处理器收到响应消息：{}", msg);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    logger.info(Constants.LOG_MSG + ctx.channel() + "【数据】处理器数据处理完毕，发送新的网页请求");
    ctx.channel().writeAndFlush(getHttpRequest());
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
    logger.error(Constants.LOG_MSG + ctx.channel() + "【数据】处理器异常：", throwable);
    ctx.channel().close();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    logger.info(Constants.LOG_MSG + ctx.channel() + "【数据】处理器连接断开：" + ctx.channel());
    super.channelInactive(ctx);
  }

  private static String getHttpRequest() throws URISyntaxException {
    StringBuffer sb = new StringBuffer("GET / HTTP/1.1")
        .append("/r/n")
        .append(DST_PROTOCOL + "://" + DST_HOST + ":" + DST_PORT)
        .append("/r/n")
        .append("/r/n");
    return sb.toString();
  }

}
