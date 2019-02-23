package com.adolphor.mynety.client.http;

import com.adolphor.mynety.client.adapter.SocksHandsShakeHandler;
import com.adolphor.mynety.client.config.Config;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.LOG_LEVEL;
import static com.adolphor.mynety.common.constants.Constants.MAX_CONTENT_LENGTH;
import static com.adolphor.mynety.common.constants.HandlerName.httpAggregatorHandler;
import static com.adolphor.mynety.common.constants.HandlerName.httpClientCodec;
import static com.adolphor.mynety.common.constants.HandlerName.httpOutBoundHandler;
import static com.adolphor.mynety.common.constants.HandlerName.loggingHandler;
import static com.adolphor.mynety.common.constants.HandlerName.socksShakerHandler;

/**
 * http 代理模式下 远程连接处理器列表
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.4
 */
@Slf4j
@ChannelHandler.Sharable
public class HttpOutBoundInitializer extends ChannelInitializer<SocketChannel> {

  public static final HttpOutBoundInitializer INSTANCE = new HttpOutBoundInitializer();

  public static void addHttpOutBoundHandler(Channel ch) {
    ch.pipeline().addAfter(loggingHandler, httpClientCodec, new HttpClientCodec());
    ch.pipeline().addAfter(httpClientCodec, httpAggregatorHandler, new HttpObjectAggregator(MAX_CONTENT_LENGTH));
    ch.pipeline().addAfter(httpAggregatorHandler, httpOutBoundHandler, HttpOutBoundHandler.INSTANCE);
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    ch.pipeline().addFirst(loggingHandler, new LoggingHandler(LOG_LEVEL));
    if (Config.HTTP_2_SOCKS5) {
      ch.pipeline().addAfter(loggingHandler, socksShakerHandler, SocksHandsShakeHandler.INSTANCE);
    } else {
      addHttpOutBoundHandler(ch);
    }
  }

}
