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
package com.shadowsocks.client.httpAdapter.tunnel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * http tunnel 代理模式下 远程处理器，连接真正的目标地址
 *
 * @author 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public class HttpTunnelRemoteHandler extends SimpleChannelInboundHandler<ByteBuf> {

  private final Channel clientProxyChannel;

  public HttpTunnelRemoteHandler(Channel clientProxyChannel) {
    this.clientProxyChannel = clientProxyChannel;
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
    if (msg.readableBytes() <= 0) {
      return;
    }
    try {
      clientProxyChannel.writeAndFlush(msg.retain());
    } catch (Exception e) {
      ctx.close();
      channelClose();
      log.error("read intenet message error", e);
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    ctx.close();
    log.info("RemoteHandler channelInactive close");
    channelClose();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ctx.close();
    channelClose();
    log.error("RemoteHandler error", cause);
  }

  private void channelClose() {
    try {
      clientProxyChannel.close();
    } catch (Exception e) {
      log.error("close channel error", e);
    }
  }
}
