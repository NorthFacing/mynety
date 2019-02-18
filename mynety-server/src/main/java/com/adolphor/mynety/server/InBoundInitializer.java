package com.adolphor.mynety.server;

import com.adolphor.mynety.common.wrapper.AbstractInBoundInitializer;
import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.LOG_LEVEL;
import static com.adolphor.mynety.common.constants.HandlerName.addressHandler;
import static com.adolphor.mynety.common.constants.HandlerName.loggingHandler;

/**
 * 初始化处理器集
 *
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
    ch.pipeline().addAfter(loggingHandler, addressHandler, AddressHandler.INSTANCE);
  }
}
