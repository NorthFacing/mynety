package com.adolphor.mynety.client.http;

import com.adolphor.mynety.common.wrapper.AbstractInBoundInitializer;
import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.LOG_LEVEL;
import static com.adolphor.mynety.common.constants.Constants.MAX_CONTENT_LENGTH;
import static com.adolphor.mynety.common.constants.HandlerName.httpAggregatorHandler;
import static com.adolphor.mynety.common.constants.HandlerName.httpProxyHandler;
import static com.adolphor.mynety.common.constants.HandlerName.httpServerCodec;
import static com.adolphor.mynety.common.constants.HandlerName.loggingHandler;

/**
 * http 代理入口 处理器列表
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.4
 */
@Slf4j
@ChannelHandler.Sharable
public final class HttpInBoundInitializer extends AbstractInBoundInitializer {

  public static final HttpInBoundInitializer INSTANCE = new HttpInBoundInitializer();

  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    super.initChannel(ch);
    ch.pipeline().addFirst(loggingHandler, new LoggingHandler(LOG_LEVEL));
    ch.pipeline().addAfter(loggingHandler, httpServerCodec, new HttpServerCodec());
    ch.pipeline().addAfter(httpServerCodec, httpAggregatorHandler, new HttpObjectAggregator(MAX_CONTENT_LENGTH));
    ch.pipeline().addAfter(httpAggregatorHandler, httpProxyHandler, HttpProxyHandler.INSTANCE);
  }

}