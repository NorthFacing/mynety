package com.adolphor.mynety.client.http;

import com.adolphor.mynety.client.adapter.SocksHandsShakeHandler;
import com.adolphor.mynety.client.config.ClientConfig;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import lombok.extern.slf4j.Slf4j;

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

  /**
   * 如果需要HTTP通过socks5加密通信，那么需要激活socks5代理。
   * -- 1. 先使用ss握手handler进行握手
   * -- 2. 再使用ss连接handler进行连接
   * socks连接建立之后，也就是在SocksConnHandler中, outRelayChannel再同时加入如下三个handler：HttpClientCodec & HttpObjectAggregator & HttpOutBoundHandler。
   * -- HttpClientCodec 是因为 inRelayChannel中加入了 HttpServerCodec 进行了转码来获取远程访问的路径地址
   * -- HttpObjectAggregator 拼装完成的HTTP请求
   * -- HttpOutBoundHandler 是最终的远程连接信息处理的handler
   * <p>
   * 如果不需要使用http转socks协议，那么直接在outRelayChannel中加入如上面一样的最终的两个处理器即可：HttpClientCodec & HttpObjectAggregator & HttpOutBoundHandler
   */
  @Override
  protected void initChannel(SocketChannel ch) throws Exception {

    if (ClientConfig.HTTP_2_SOCKS5) {
      ch.pipeline().addLast(SocksHandsShakeHandler.INSTANCE);
    } else {
      ch.pipeline().addLast(new HttpClientCodec());
      ch.pipeline().addLast(new HttpObjectAggregator(6553600));
      ch.pipeline().addLast(HttpOutBoundHandler.INSTANCE);
    }

  }

}
