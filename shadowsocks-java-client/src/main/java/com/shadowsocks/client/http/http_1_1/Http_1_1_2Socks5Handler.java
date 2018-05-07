/**
 * MIT License
 * <p>
 * Copyright (c) Bob.Zhu
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
package com.shadowsocks.client.http.http_1_1;

import com.shadowsocks.client.http.HttpOutboundInitializer;
import com.shadowsocks.common.bean.Address;
import com.shadowsocks.common.constants.Constants;
import com.shadowsocks.common.nettyWrapper.TempAbstractInRelayHandler;
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

import java.util.regex.Matcher;

import static com.shadowsocks.client.config.ClientConfig.HTTP_2_SOCKS5;
import static com.shadowsocks.client.config.ClientConfig.SOCKS_PROXY_PORT;
import static com.shadowsocks.common.constants.Constants.LOG_MSG;
import static com.shadowsocks.common.constants.Constants.LOG_MSG_OUT;
import static com.shadowsocks.common.constants.Constants.LOOPBACK_ADDRESS;
import static com.shadowsocks.common.constants.Constants.PATH_PATTERN;
import static com.shadowsocks.common.constants.Constants.REQUEST_ADDRESS;
import static com.shadowsocks.common.constants.Constants.REQUEST_TEMP_LIST;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * http 代理模式下 主处理器
 * <p>
 * channel中的数据类型都经过HTTP编解码器解析，所以都是 HttpObject 类型
 * （后面再考虑效率问题吧，主要是缓存中的数据类型不好处理）
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public class Http_1_1_2Socks5Handler extends TempAbstractInRelayHandler<Object> {

  private DefaultHttpRequest httpRequest;

  public Http_1_1_2Socks5Handler(DefaultHttpRequest httpRequest) {
    this.httpRequest = httpRequest; // 解析出地址，建立socks连接
    requestTempLists.add(httpRequest); // 连接建立之后，转发到目标地址
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.info("[ {}{}{} ] {} channel active...", ctx.channel(), LOG_MSG, remoteChannelRef.get(), getSimpleName(this));
    Channel clientChannel = ctx.channel();

    Address address = resolveHttpProxyPath(httpRequest.uri());
    clientChannel.attr(REQUEST_ADDRESS).set(address);
    clientChannel.attr(REQUEST_TEMP_LIST).set(requestTempLists);

    Bootstrap remoteBootStrap = new Bootstrap();
    remoteBootStrap.group(clientChannel.eventLoop())
        .channel(Constants.channelClass)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)
        .handler(new HttpOutboundInitializer(this, clientChannel));

    String connHost;
    int connPort;
    if (HTTP_2_SOCKS5) {
      connHost = LOOPBACK_ADDRESS;
      connPort = SOCKS_PROXY_PORT;
    } else {
      connHost = address.getHost();
      connPort = address.getPort();
    }
    try {
      ChannelFuture channelFuture = remoteBootStrap.connect(connHost, connPort);
      channelFuture.addListener((ChannelFutureListener) future -> {
        if (future.isSuccess()) {
          Channel remoteChannel = future.channel();
          remoteChannelRef.set(remoteChannel);
          logger.debug("[ {}{}{} ] http1.1 connect success: outHost = {}, outPort = {}", clientChannel, LOG_MSG, remoteChannel, connHost, connPort);
        } else {
          logger.debug("[ {}{}{} ] http1.1 connect failed: outHost = {}, outPort = {}", clientChannel, LOG_MSG, clientChannel, connHost, connPort);
          super.releaseHttpObjectsTemp();
          future.cancel(true);
          channelClose(ctx);
        }
      });
    } catch (Exception e) {
      logger.error("[ " + clientChannel + LOG_MSG + connHost + ":" + connPort + " ] http1.1 connect internet error", e);
      channelClose(ctx);
    }

  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    logger.debug("[ {}{}{} ] http1.1 to socks handler receive http msg: {}", ctx.channel(), LOG_MSG, remoteChannelRef.get(), msg);
    Channel remoteChannel = remoteChannelRef.get();
    synchronized (requestTempLists) {
      if (isConnected) {
        ReferenceCountUtil.retain(msg);
        remoteChannel.writeAndFlush(msg);
        logger.debug("[ {}{}{} ] transfer http1.1 request to socks: {}", ctx.channel(), LOG_MSG_OUT, remoteChannel, msg);
      } else {
        if (msg instanceof HttpObject) {
          ReferenceCountUtil.retain(msg);
          requestTempLists.add(msg);
          logger.debug("[ {}{}{} ] add transfer http1.1 request to temp list: {}", ctx.channel(), LOG_MSG, remoteChannel, msg);
        } else {
          logger.warn("[ {}{}{} ] http1.1 unhandled msg type: {}", ctx.channel(), LOG_MSG, remoteChannel, msg);
        }
      }
    }
  }

  private Address resolveHttpProxyPath(String address) {
    Matcher matcher = PATH_PATTERN.matcher(address);
    if (matcher.find()) {
      String scheme = matcher.group(1);
      String host = matcher.group(2);
      int port = resolvePort(scheme, matcher.group(4));
      String path = matcher.group(5);
      return new Address(scheme, host, port, path);
    } else {
      throw new IllegalStateException("Illegal http proxy path: " + address);
    }
  }

  private int resolvePort(String scheme, String port) {
    if (StringUtils.isEmpty(port)) {
      return "https".equals(scheme) ? 443 : 80;
    }
    return Integer.parseInt(port);
  }

}
