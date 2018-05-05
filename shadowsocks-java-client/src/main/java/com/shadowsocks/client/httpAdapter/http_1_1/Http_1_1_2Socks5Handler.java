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
package com.shadowsocks.client.httpAdapter.http_1_1;

import com.shadowsocks.client.config.ClientConfig;
import com.shadowsocks.client.httpAdapter.HttpOutboundInitializer;
import com.shadowsocks.common.bean.Address;
import com.shadowsocks.common.constants.Constants;
import com.shadowsocks.common.nettyWrapper.TempSimpleChannelInboundHandler;
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

import static com.shadowsocks.common.constants.Constants.HTTP_REQUEST;
import static com.shadowsocks.common.constants.Constants.LOG_MSG;
import static com.shadowsocks.common.constants.Constants.LOG_MSG_OUT;
import static com.shadowsocks.common.constants.Constants.PATH_PATTERN;
import static com.shadowsocks.common.constants.Constants.REQUEST_ADDRESS;
import static com.shadowsocks.common.constants.Constants.REQUEST_TEMP_LIST;
import static com.shadowsocks.common.constants.Constants.SOCKS5_CONNECTED;
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
public class Http_1_1_2Socks5Handler extends TempSimpleChannelInboundHandler<HttpObject> {

  private boolean firstRequest = true; // 是否是第一次请求

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.info("[ {}{}{} ] {} channel active...", ctx.channel(), LOG_MSG, remoteChannelRef.get(), getSimpleName(this));
    Channel clientChannel = ctx.channel();

    DefaultHttpRequest httpRequest = ctx.channel().attr(HTTP_REQUEST).get();

    Address fullPath = resolveHttpProxyPath(httpRequest.uri());
    clientChannel.attr(REQUEST_ADDRESS).set(fullPath);
    clientChannel.attr(REQUEST_TEMP_LIST).set(requestTempLists);

    Bootstrap remoteBootStrap = new Bootstrap();
    remoteBootStrap.group(clientChannel.eventLoop())
        .channel(Constants.channelClass)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)
        .handler(new HttpOutboundInitializer(clientChannel, this));

    String connHost;
    int connPort;
    if (ClientConfig.HTTP_2_SOCKS5) {
      connHost = ClientConfig.LOCAL_ADDRESS;
      connPort = ClientConfig.SOCKS_LOCAL_PORT;
    } else {
      connHost = fullPath.getHost();
      connPort = fullPath.getPort();
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
  protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
    Channel remoteChannel = remoteChannelRef.get();
    synchronized (requestTempLists) {
      if (remoteChannel != null && Boolean.valueOf(remoteChannel.attr(SOCKS5_CONNECTED).get())) {
        ReferenceCountUtil.retain(msg);
        remoteChannel.writeAndFlush(msg);
        logger.debug("[ {}{}{} ] transfer http1.1 request to socks: {}", ctx.channel(), LOG_MSG_OUT, remoteChannel, msg);
      } else {
        if (msg instanceof HttpObject) {
          ReferenceCountUtil.retain(msg);
          requestTempLists.add(msg);
          logger.debug("[ {}{}{} ] add transfer http1.1 request to temp list: {}", ctx.channel(), LOG_MSG, remoteChannel, msg);
        } else {
          logger.warn("[ {}{}{} ] unhandled msg type: {}", ctx.channel(), LOG_MSG, remoteChannel, msg);
        }
      }
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    Channel remoteChannel = remoteChannelRef.get();
    // 先消费完当前缓存的数据，然后将后续的消息直接透传即可
    // ———— 1. 消费当前缓存消息（基本上应该是建立CONNECT连接所需要的消息）
    logger.debug("[ {}{}{} ] consume temp http1.1 request over socks5 to dst host, msg num: {}", ctx.channel(), LOG_MSG_OUT, remoteChannel, requestTempLists.size());
    super.consumeHttpObjectsTemp(remoteChannel);
    if (firstRequest && remoteChannel != null
        && Boolean.valueOf(remoteChannel.attr(SOCKS5_CONNECTED).get())) {
      // ———— 2. 一般HTTP代理无需建立成功的时候返回200，只需要将最后的结果返回透传即可
      // ———— 3. 移除 inbound 和 outbound 双方的编解码（移除可以提效，不移除可以编辑请求头信息）
//      ctx.channel().pipeline().remove(HttpServerCodec.class);
//      logger.debug("[ {}{}{} ] http1.1 clientChannel remove handler: HttpClientCodec", ctx.channel(), LOG_MSG, remoteChannel);
//      remoteChannel.pipeline().remove(HttpClientCodec.class);
//      logger.debug("[ {}{}{} ] http1.1 remoteChannel remove handler: HttpClientCodec", ctx.channel(), LOG_MSG, remoteChannel);
      firstRequest = false;
    }
  }

  private Address resolveHttpProxyPath(String fullPath) {
    Matcher matcher = PATH_PATTERN.matcher(fullPath);
    if (matcher.find()) {
      String scheme = matcher.group(1);
      String host = matcher.group(2);
      int port = resolvePort(scheme, matcher.group(4));
      String path = matcher.group(5);
      return new Address(scheme, host, port, path);
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
