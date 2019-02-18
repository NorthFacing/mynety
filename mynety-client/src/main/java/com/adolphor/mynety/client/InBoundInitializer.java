package com.adolphor.mynety.client;

import com.adolphor.mynety.common.wrapper.AbstractInBoundInitializer;
import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.LOG_LEVEL;
import static com.adolphor.mynety.common.constants.HandlerName.loggingHandler;
import static com.adolphor.mynety.common.constants.HandlerName.socksAuthHandler;
import static com.adolphor.mynety.common.constants.HandlerName.socksPortUnificationServerHandler;

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
  protected void initChannel(SocketChannel ch) throws Exception {
    super.initChannel(ch);
    ch.pipeline().addFirst(loggingHandler, new LoggingHandler(LOG_LEVEL));
    ch.pipeline().addAfter(loggingHandler, socksPortUnificationServerHandler, new SocksPortUnificationServerHandler());
    ch.pipeline().addAfter(socksPortUnificationServerHandler, socksAuthHandler, AuthHandler.INSTANCE);
  }
}