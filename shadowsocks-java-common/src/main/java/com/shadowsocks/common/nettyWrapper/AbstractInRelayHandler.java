/**
 * MIT License
 * <p>
 * Copyright (c) Bob.Zhu
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
package com.shadowsocks.common.nettyWrapper;

import com.shadowsocks.common.utils.SocksServerUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 带有缓存的本地连接处理器：
 * 1. 增加 requestTempLists 用于缓存远程连接未成功之前客户端发送的请求信息
 * 2. 增加可供手动调用的释放方法
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public abstract class AbstractInRelayHandler<I> extends AbstractSimpleHandler<I> {

  /**
   * 建立socks5连接需要时间，此字段标记远程连接是否建立完成
   */
  protected boolean isConnected = false;
  protected AtomicReference<Channel> remoteChannelRef = new AtomicReference<>();

  /**
   * 缓存请求：
   * 1.HTTP请求下，HttpRequest,HttpContent,LastHttpContent 分开请求的情况
   * 2.HTTP请求下，并发请求的情况
   * 3.socks请求下，黏包的问题
   */
  protected final List<Object> requestTempLists = new LinkedList();

  /**
   * 释放HTTP相关缓存
   */
  public void releaseHttpObjectsTemp() {
    synchronized (requestTempLists) {
      requestTempLists.forEach(msg -> ReferenceCountUtil.release(msg));
      requestTempLists.clear();
    }
  }

  public void setConnected(boolean isConnected) {
    this.isConnected = isConnected;
  }

  @Override
  protected void channelClose(ChannelHandlerContext ctx) {
    SocksServerUtils.flushOnClose(ctx.channel());
    SocksServerUtils.flushOnClose(remoteChannelRef.get());
  }


}
