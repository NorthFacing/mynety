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
package com.shadowsocks.client.httpAdapter.http_1_1;

import com.shadowsocks.common.constants.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpObject;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;

import static com.shadowsocks.common.constants.Constants.HTTP_REQUEST;
import static com.shadowsocks.common.constants.Constants.LOG_MSG;
import static com.shadowsocks.common.constants.Constants.PATH_PATTERN;

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
 * http 代理模式下 主处理器
 *
 * @author 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public class Http_1_1_Handler extends SimpleChannelInboundHandler<HttpObject> {

  private AtomicReference<Channel> remoteChannelRef = new AtomicReference<>();
  DefaultHttpRequest httpRequest;
  FullPath fullPath;
  List<HttpObject> requestTempLists = new LinkedList();

  @Override
  public void channelActive(ChannelHandlerContext clientCtx) throws Exception {
    httpRequest = (DefaultHttpRequest) clientCtx.channel().attr(HTTP_REQUEST).get();
    fullPath = resolveHttpProxyPath(httpRequest.uri());

    Channel clientChannel = clientCtx.channel();

    Bootstrap remoteBootStrap = new Bootstrap();
    remoteBootStrap.group(clientChannel.eventLoop())
        .channel(Constants.channelClass)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)
        .handler(new HttpOutboundInitializer(clientChannel));

    try {
      ChannelFuture channelFuture = remoteBootStrap.connect(fullPath.host, fullPath.port);
      channelFuture.addListener((ChannelFutureListener) future -> {
        if (future.isSuccess()) {
          Channel remoteChannel = future.channel();
          remoteChannelRef.set(remoteChannel);
          log.debug("{} {} connect success proxyHost/dstAddr = {}, proxyPort/dstPort = {}", LOG_MSG, remoteChannel, fullPath.host, fullPath.port);
          remoteChannel.writeAndFlush(httpRequest);
          synchronized (requestTempLists) {
            requestTempLists.forEach(msg -> {
              remoteChannel.writeAndFlush(msg);
              log.debug("{} {} send http request msg after bind: {}", LOG_MSG, remoteChannel, msg);
            });
          }
        } else {
          log.debug("{} {} connect fail proxyHost/dstAddr = {}, proxyPort/dstPort = {}", LOG_MSG, clientChannel, fullPath.host, fullPath.port);
          ReferenceCountUtil.release(httpRequest);
          synchronized (requestTempLists) {
            requestTempLists.forEach(msg -> ReferenceCountUtil.release(msg));
          }
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
  protected void channelRead0(ChannelHandlerContext clientCtx, HttpObject msg) throws Exception {
    Channel remoteChannel = remoteChannelRef.get();
    synchronized (requestTempLists) {
      if (remoteChannel == null) {
        requestTempLists.add(msg);
        log.debug("{} add transfer http request to temp list: {}", LOG_MSG, fullPath);
      } else {
        remoteChannel.writeAndFlush(msg);
        log.debug("{} transfer http request to dst host: {}", LOG_MSG, fullPath);
      }
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

  private FullPath resolveHttpProxyPath(String fullPath) {
    Matcher matcher = PATH_PATTERN.matcher(fullPath);
    if (matcher.find()) {
      String scheme = matcher.group(1);
      String host = matcher.group(2);
      int port = resolvePort(scheme, matcher.group(4));
      String path = matcher.group(5);
      return new FullPath(scheme, host, port, path);
    } else {
      throw new IllegalStateException("Illegal http proxy path: " + fullPath);
    }
  }

  private int resolvePort(String scheme, String port) {
    if (StringUtils.isEmpty(port)) {
      return "https".equals(scheme) ? 443 : 80;
    }
    return Integer.parseInt(port);
  }

  private class FullPath {
    private String scheme;
    private String host;
    private int port;
    private String path;

    private FullPath(String scheme, String host, int port, String path) {
      this.scheme = scheme;
      this.host = host;
      this.port = port;
      this.path = path;
    }

    @Override
    public String toString() {
      return "FullPath{" +
          "scheme='" + scheme + '\'' +
          ", host='" + host + '\'' +
          ", port=" + port +
          ", path='" + path + '\'' +
          '}';
    }
  }


}
