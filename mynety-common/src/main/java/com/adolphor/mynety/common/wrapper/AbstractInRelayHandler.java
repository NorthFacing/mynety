package com.adolphor.mynety.common.wrapper;

import com.adolphor.mynety.common.utils.SocksServerUtils;
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
   * 释放缓存的请求信息
   */
  public void releaseRequestTempLists() {
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
    SocksServerUtils.closeOnFlush(ctx.channel());
    SocksServerUtils.closeOnFlush(remoteChannelRef.get());
  }


}
