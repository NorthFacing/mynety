/**
 * MIT License
 * <p>
 * Copyright (c) 2018 0haizhu0@gmail.com
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.shadowsocks.client.httpAdapter;

import com.shadowsocks.client.httpAdapter.http_1_1.Http_1_1_2Socks5Handler;
import com.shadowsocks.client.httpAdapter.http_1_1.Http_1_1_Handler;
import com.shadowsocks.client.httpAdapter.http_1_1.Http_1_1_RemoteHandler;
import com.shadowsocks.client.httpAdapter.tunnel.HttpTunnel2Socks5Handler;
import com.shadowsocks.client.httpAdapter.tunnel.HttpTunnelHandler;
import com.shadowsocks.client.httpAdapter.tunnel.HttpTunnelRemoteHandler;
import com.shadowsocks.client.socks5Wrapper.SocksWrapperHandsShakeHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;

import java.util.LinkedList;

import static com.shadowsocks.common.constants.Constants.EXTRA_OUT_RELAY_HANDLER;

/**
 * http 代理模式下 远程连接处理器列表
 *
 * @author 0haizhu0@gmail.com
 * @since v0.0.4
 */
public class HttpOutboundInitializer extends ChannelInitializer<SocketChannel> {

  private Channel clientChannel;
  private ChannelHandlerAdapter handler;

  public HttpOutboundInitializer(Channel clientChannel, SimpleChannelInboundHandler handler) {
    this.clientChannel = clientChannel;
    this.handler = handler;
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    LinkedList<ChannelHandler> handlers = new LinkedList<>();
    // HTTP1.1
    if (handler instanceof Http_1_1_2Socks5Handler) {
      ch.pipeline().addLast(new SocksWrapperHandsShakeHandler(clientChannel));
      handlers.add(new HttpClientCodec());
      ch.pipeline().addLast(new HttpRemoteHandler(clientChannel));
    } else if (handler instanceof Http_1_1_Handler) {
      ch.pipeline().addLast(new HttpClientCodec());
      ch.pipeline().addLast(new Http_1_1_RemoteHandler(clientChannel));
    }
    // HTTP tunnel
    else if (handler instanceof HttpTunnel2Socks5Handler) {
      ch.pipeline().addLast(new SocksWrapperHandsShakeHandler(clientChannel));
      ch.pipeline().addLast(new HttpTunnelRemoteHandler(clientChannel));
    } else if (handler instanceof HttpTunnelHandler) {
      ch.pipeline().addLast(new HttpTunnelRemoteHandler(clientChannel));
    }
    clientChannel.attr(EXTRA_OUT_RELAY_HANDLER).set(handlers);
  }

}
