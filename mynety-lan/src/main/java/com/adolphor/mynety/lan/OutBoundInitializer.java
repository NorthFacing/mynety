package com.adolphor.mynety.lan;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.LOG_LEVEL;
import static com.adolphor.mynety.common.constants.HandlerName.loggingHandler;
import static com.adolphor.mynety.common.constants.HandlerName.outBoundHandler;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.6
 */
@Slf4j
@ChannelHandler.Sharable
public class OutBoundInitializer extends ChannelInitializer<SocketChannel> {

  public static final OutBoundInitializer INSTANCE = new OutBoundInitializer();

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    ch.pipeline().addFirst(loggingHandler, new LoggingHandler(LOG_LEVEL));
    ch.pipeline().addAfter(loggingHandler, outBoundHandler, OutBoundHandler.INSTANCE);
  }
}
