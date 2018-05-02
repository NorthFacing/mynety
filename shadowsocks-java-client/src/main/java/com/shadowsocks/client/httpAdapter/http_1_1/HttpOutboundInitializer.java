package com.shadowsocks.client.httpAdapter.http_1_1;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;

/**
 * http 代理模式下 远程连接处理器列表
 *
 * @author 0haizhu0@gmail.com
 * @since v0.0.4
 */
public class HttpOutboundInitializer extends ChannelInitializer<SocketChannel> {

  private Channel clientChannel;

  public HttpOutboundInitializer(Channel clientChannel) {
    this.clientChannel = clientChannel;
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    // HttpClientCodec 相当于 HttpResponseDecoder && HttpRequestEncoder 一起的作用，
    ch.pipeline().addLast(new HttpClientCodec());
    ch.pipeline().addLast(new Http_1_1_RemoteHandler(clientChannel));
  }

}
