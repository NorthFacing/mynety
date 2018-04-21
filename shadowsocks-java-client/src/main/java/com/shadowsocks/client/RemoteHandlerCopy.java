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
package com.shadowsocks.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;

/**
 * 远程连接处理器，连接代理服务器服务端。
 * 从 v0.0.4 开始，废弃使用
 *
 * @author 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Deprecated
@Slf4j
public final class RemoteHandlerCopy extends ChannelInboundHandlerAdapter {

  private final Promise<Channel> promise;

  public RemoteHandlerCopy(Promise<Channel> promise) {
    this.promise = promise;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    ctx.pipeline().remove(this);
    promise.setSuccess(ctx.channel()); // 连接到指定地址成功后，setSuccess 让 Promise 的回调函数执行；在这个 Promise 中放有一个连接远程的 Channel
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
    log.error(LOG_MSG + " inboundChannel=" + ctx.channel() + " 和 outboundChannel=" + promise.getNow() + " 关联出错：", throwable);
    promise.setFailure(throwable);
  }
}
