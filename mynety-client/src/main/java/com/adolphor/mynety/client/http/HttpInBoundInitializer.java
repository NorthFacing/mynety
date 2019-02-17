package com.adolphor.mynety.client.http;

import com.adolphor.mynety.common.wrapper.AbstractInBoundInitializer;
import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.client.constants.ClientConstants.MAX_CONTENT_LENGTH;
import static com.adolphor.mynety.client.constants.ClientConstants.httpAggregator;
import static com.adolphor.mynety.client.constants.ClientConstants.httpProxy;
import static com.adolphor.mynety.client.constants.ClientConstants.httpServerCodec;

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
    ch.pipeline().addFirst(httpServerCodec, new HttpServerCodec());
    ch.pipeline().addAfter(httpServerCodec, httpAggregator, new HttpObjectAggregator(MAX_CONTENT_LENGTH));
    ch.pipeline().addAfter(httpAggregator, httpProxy, HttpProxyHandler.INSTANCE);
  }

}