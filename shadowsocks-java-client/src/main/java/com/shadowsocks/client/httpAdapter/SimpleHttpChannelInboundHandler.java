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
package com.shadowsocks.client.httpAdapter;

import io.netty.channel.Channel;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;

/**
 * Http消息处理器
 * 1. 增加 requestTempLists 用于缓存 HttpRequest, HttpContent, LastHttpContent 请求信息
 * 2. 增加缓存消费和释放方法
 *
 * @param <I> 当前channel接收到的数据类型
 * @author 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public abstract class SimpleHttpChannelInboundHandler<I> extends SimpleChannelInboundHandler<I> {

  protected List requestTempLists = new LinkedList();

  /**
   * 消费之前缓存的HTTP相关请求
   *
   * @param remoteChannel outboundChannel，连接远程服务器的channel
   */
  public void consumeHttpObjectsTemp(Channel remoteChannel) {
    synchronized (requestTempLists) {
      requestTempLists.forEach(msg -> {
        remoteChannel.writeAndFlush(msg);
        logger.debug("{} {} consume temp httpObjects: {}", LOG_MSG, remoteChannel, msg);
      });
    }
  }

  /**
   * 释放HTTP相关缓存
   */
  public void releaseHttpObjectsTemp() {
    synchronized (requestTempLists) {
      requestTempLists.forEach(msg -> ReferenceCountUtil.release(msg));
    }
  }

}
