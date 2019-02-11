package com.adolphor.mynety.client;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * OutBound 初始化处理器集
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
@ChannelHandler.Sharable
public class OutBoundInitializer extends ChannelInitializer<SocketChannel> {

  public static final OutBoundInitializer INSTANCE = new OutBoundInitializer();

  @Override
  protected void initChannel(SocketChannel ch) {
    ch.pipeline().addLast(OutBoundHandler.INSTANCE);
  }
}
