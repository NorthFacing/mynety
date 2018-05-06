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

import io.netty.channel.Channel;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;

import static com.shadowsocks.common.constants.Constants.LOG_MSG_OUT;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * 带有缓存的本地连接处理器：
 * 1. 增加 requestTempLists 用于缓存远程连接未成功之前客户端发送的请求信息
 * 2. 增加缓存消费和释放方法
 * 3. 远程连接完成之后，需要手动调用 afterConn 方法
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public abstract class TempAbstractInRelayHandler<I> extends AbstractInRelayHandler<I> {

  /**
   * 建立socks5连接需要时间，此字段标记远程连接是否建立完成
   */
  protected boolean isConnected = false;

  /**
   * 缓存请求：
   * 1.HTTP请求下，HttpRequest,HttpContent,LastHttpContent 分开请求的情况
   * 2.HTTP请求下，并发请求的情况
   * 3.socks请求下，黏包的问题
   */
  protected final List<Object> requestTempLists = new LinkedList();


  /**
   * 远程连接建立成功之后的回调方法
   */
  public void afterConn(Channel clientChannel) {
    consumeHttpObjectsTemp();
  }

  /**
   * 消费之前缓存的HTTP相关请求
   */
  private void consumeHttpObjectsTemp() {
    synchronized (requestTempLists) {
      requestTempLists.forEach(msg -> {
        remoteChannelRef.get().writeAndFlush(msg);
        logger.debug("[ {}{} ] [{}] consume temp httpObjects: {}", LOG_MSG_OUT, remoteChannelRef.get(), getSimpleName(this), msg);
      });
      requestTempLists.clear();
    }
  }

  /**
   * 释放HTTP相关缓存
   */
  public void releaseHttpObjectsTemp() {
    synchronized (requestTempLists) {
      requestTempLists.forEach(msg -> ReferenceCountUtil.release(msg));
      requestTempLists.clear();
    }
  }

}
