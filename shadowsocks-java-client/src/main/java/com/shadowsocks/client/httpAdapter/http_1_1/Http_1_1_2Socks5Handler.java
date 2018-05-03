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

import com.shadowsocks.client.config.ClientConfig;
import com.shadowsocks.client.httpAdapter.HttpOutboundInitializer;
import com.shadowsocks.client.httpAdapter.SimpleHttpChannelInboundHandler;
import com.shadowsocks.common.bean.FullPath;
import com.shadowsocks.common.constants.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpObject;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;

import static com.shadowsocks.common.constants.Constants.HTTP_REQUEST;
import static com.shadowsocks.common.constants.Constants.HTTP_REQUEST_FULLPATH;
import static com.shadowsocks.common.constants.Constants.LOG_MSG;
import static com.shadowsocks.common.constants.Constants.PATH_PATTERN;
import static com.shadowsocks.common.constants.Constants.SOCKS5_CONNECTED;

/**
 * http 代理模式下 主处理器
 *
 * @author 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public class Http_1_1_2Socks5Handler extends SimpleHttpChannelInboundHandler<HttpObject> {

  private AtomicReference<Channel> remoteChannelRef = new AtomicReference<>();

  @Override
  public void channelActive(ChannelHandlerContext clientCtx) throws Exception {
    Channel clientChannel = clientCtx.channel();

    DefaultHttpRequest httpRequest = clientChannel.attr(HTTP_REQUEST).get();
    FullPath fullPath = resolveHttpProxyPath(httpRequest.uri());
    clientChannel.attr(HTTP_REQUEST_FULLPATH).set(fullPath);

    Bootstrap remoteBootStrap = new Bootstrap();
    remoteBootStrap.group(clientChannel.eventLoop())
        .channel(Constants.channelClass)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)
        .handler(new HttpOutboundInitializer(clientChannel, this));

    try {
      String connHost;
      int connPort;
      if (ClientConfig.HTTP_2_SOCKS5) {
        connHost = ClientConfig.LOCAL_ADDRESS;
        connPort = ClientConfig.SOCKS_LOCAL_PORT;
      } else {
        connHost = fullPath.getHost();
        connPort = fullPath.getPort();
      }
      ChannelFuture channelFuture = remoteBootStrap.connect(connHost, connPort);
      channelFuture.addListener((ChannelFutureListener) future -> {
        requestTempLists.add(httpRequest);
        if (future.isSuccess()) {
          Channel remoteChannel = future.channel();
          remoteChannelRef.set(remoteChannel);
          logger.debug("{} {} http1.1 connect success proxyHost/dstAddr = {}, proxyPort/dstPort = {}", LOG_MSG, remoteChannel, connHost, connPort);
        } else {
          logger.debug("{} {} http1.1 connect fail proxyHost/dstAddr = {}, proxyPort/dstPort = {}", LOG_MSG, clientChannel, connHost, connPort);
          super.releaseHttpObjectsTemp();
          future.cancel(true);
          channelClose();
        }
      });
    } catch (Exception e) {
      logger.error("http1.1 connect internet error", e);
      channelClose();
    }

  }

  @Override
  protected void channelRead0(ChannelHandlerContext clientCtx, HttpObject msg) throws Exception {
    Channel remoteChannel = remoteChannelRef.get();
    synchronized (requestTempLists) {
      ReferenceCountUtil.retain(msg);
      // 确保socks5连接成功再开始盲转数据
      if (remoteChannel == null || remoteChannel.attr(SOCKS5_CONNECTED).get() == null || !remoteChannel.attr(SOCKS5_CONNECTED).get()) {
        requestTempLists.add(msg);
        logger.debug("{} add transfer http request to temp list: {}", LOG_MSG, msg);
      } else {
        remoteChannel.writeAndFlush(msg);
        logger.debug("{} transfer http request to dst host: {}", LOG_MSG, msg);
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
      logger.error(LOG_MSG + "close channel error", e);
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

}
