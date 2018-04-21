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
package com.shadowsocks.client.httpAdapter.tunnel;

import com.shadowsocks.common.constants.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;

import static com.shadowsocks.common.constants.Constants.CONNECTION_ESTABLISHED;
import static com.shadowsocks.common.constants.Constants.HTTP_REQUEST;
import static com.shadowsocks.common.constants.Constants.LOG_MSG;
import static com.shadowsocks.common.constants.Constants.TUNNEL_ADDR_PATTERN;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

//import io.netty.handler.codec.http.HttpMethod;
//import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
//import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
//import org.apache.http.HttpHost;
//import org.apache.http.client.methods.CloseableHttpResponse;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.client.protocol.HttpClientContext;
//import org.apache.http.config.Registry;
//import org.apache.http.config.RegistryBuilder;
//import org.apache.http.conn.socket.ConnectionSocketFactory;
//import org.apache.http.conn.socket.PlainConnectionSocketFactory;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
//import org.apache.http.ssl.SSLContexts;
//import java.net.InetSocketAddress;
//import static com.shadowsocks.client.config.ClientConfig.SOCKS_LOCAL_PORT;
//import static com.shadowsocks.common.constants.Constants.SOCKS_ADDR_FOR_HTTP;

/**
 * http套上socks代理进行通信
 *
 * @author 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public class HttpTunnelHandler extends SimpleChannelInboundHandler {

  private AtomicReference<Channel> remoteChannelRef = new AtomicReference<>();
  HttpRequest httpRequest;
  Address address;

  @Override
  public void channelActive(ChannelHandlerContext clientCtx) throws Exception {
    httpRequest = clientCtx.channel().attr(HTTP_REQUEST).get();
    address = resolveTunnelAddr(httpRequest.uri());
    ReferenceCountUtil.release(httpRequest); // 手动消费，防止内存泄漏

    Channel clientChannel = clientCtx.channel();

    Bootstrap remoteBootStrap = new Bootstrap();
    remoteBootStrap.group(clientChannel.eventLoop())
        .channel(Constants.channelClass)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast(new HttpTunnelRemoteHandler(clientCtx));
          }
        });

    try {
      ChannelFuture channelFuture = remoteBootStrap.connect(address.host, address.port);
      channelFuture.addListener((ChannelFutureListener) future -> {
        if (future.isSuccess()) {
          remoteChannelRef.set(future.channel());

          // 告诉客户端建立隧道成功
          DefaultHttpResponse response = new DefaultHttpResponse(HTTP_1_1, CONNECTION_ESTABLISHED);
          clientCtx.channel().writeAndFlush(response);

          // 建立tunnel隧道成功之后，就不再需要以下处理器了，之后的数据全部使用隧道盲转
          clientCtx.pipeline().remove(HttpServerCodec.class);
          log.debug("{} {} remove handler: serverCodec", LOG_MSG, clientChannel);
          clientCtx.pipeline().remove(HttpObjectAggregator.class);
          log.debug("{} {} remove handler: aggregator", LOG_MSG, clientChannel);

          log.debug("connect success proxyHost/dstAddr = {}, proxyPort/dstPort = {}", address.host, address.port);
        } else {
          log.debug("connect fail proxyHost/dstAddr = {}, proxyPort/dstPort = {}", address.host, address.port);
          future.cancel(true);
          channelClose();
        }
      });
    } catch (Exception e) {
      log.error("connect intenet error", e);
      channelClose();
    }

  }

  @Override
  protected void channelRead0(ChannelHandlerContext clientCtx, Object msg) throws Exception {
    ByteBuf byteBuf = (ByteBuf) msg;
    if (byteBuf.readableBytes() <= 0) {
      return;
    }
    try {
      if (!byteBuf.hasArray()) {
        int len = byteBuf.readableBytes();
        byte[] arr = new byte[len];
        byteBuf.getBytes(0, arr);
        remoteChannelRef.get().writeAndFlush(Unpooled.wrappedBuffer(arr));
      }
    } catch (Exception e) {
      log.error(LOG_MSG + clientCtx.channel() + " Send data to remoteServer error: ", e);
    }

  }

  @SuppressWarnings("Duplicates")
  private void channelClose() {
    try {
      if (remoteChannelRef.get() != null) {
        remoteChannelRef.get().close();
        remoteChannelRef = null;
      }
    } catch (Exception e) {
      log.error(LOG_MSG + "close channel error", e);
    }
  }

  private Address resolveTunnelAddr(String addr) {
    Matcher matcher = TUNNEL_ADDR_PATTERN.matcher(addr);
    if (matcher.find()) {
      return new Address(matcher.group(1), Integer.parseInt(matcher.group(2)));
    } else {
      throw new IllegalStateException("Illegal tunnel addr: " + addr);
    }
  }

  private class Address {
    private String host;
    private int port;

    public Address(String host, int port) {
      this.host = host;
      this.port = port;
    }

    public String getHost() {
      return host;
    }

    public int getPort() {
      return port;
    }

    @Override
    public String toString() {
      return String.format("%s:%d", host, port);
    }

  }

}