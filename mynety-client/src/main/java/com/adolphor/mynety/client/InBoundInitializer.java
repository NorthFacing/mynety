package com.adolphor.mynety.client;

import com.adolphor.mynety.common.wrapper.AbstractInBoundInitializer;
import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
@ChannelHandler.Sharable
public final class InBoundInitializer extends AbstractInBoundInitializer {

  public static final InBoundInitializer INSTANCE = new InBoundInitializer();

  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    super.initChannel(ch);
    ch.pipeline().addLast(new SocksPortUnificationServerHandler());
    ch.pipeline().addLast(AuthHandler.INSTANCE);
  }
}