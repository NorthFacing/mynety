package com.adolphor.mynety.client.http;

import com.adolphor.mynety.client.adapter.SocksHandsShakeHandler;
import com.adolphor.mynety.client.config.ClientConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.client.constants.ClientConstants.MAX_CONTENT_LENGTH;
import static com.adolphor.mynety.client.constants.ClientConstants.httpAggregator;
import static com.adolphor.mynety.client.constants.ClientConstants.httpClientCodec;
import static com.adolphor.mynety.client.constants.ClientConstants.httpOutBound;
import static com.adolphor.mynety.client.constants.ClientConstants.socksShaker;

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
    ch.pipeline().addFirst(httpClientCodec, new HttpClientCodec());
    ch.pipeline().addAfter(httpClientCodec, httpAggregator, new HttpObjectAggregator(MAX_CONTENT_LENGTH));
    ch.pipeline().addAfter(httpAggregator, httpOutBound, HttpOutBoundHandler.INSTANCE);
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    if (ClientConfig.HTTP_2_SOCKS5) {
      ch.pipeline().addFirst(socksShaker, SocksHandsShakeHandler.INSTANCE);
    } else {
      addHttpOutBoundHandler(ch);
    }
  }

}
