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
package com.shadowsocks.client.httpAdapter.tunnel;

import com.shadowsocks.client.config.ClientConfig;
import com.shadowsocks.client.httpAdapter.HttpOutboundInitializer;
import com.shadowsocks.common.bean.Address;
import com.shadowsocks.common.constants.Constants;
import com.shadowsocks.common.nettyWrapper.TempAbstractInRelayHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpObject;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;
import static com.shadowsocks.common.constants.Constants.LOG_MSG_OUT;
import static com.shadowsocks.common.constants.Constants.REQUEST_ADDRESS;
import static com.shadowsocks.common.constants.Constants.TUNNEL_ADDR_PATTERN;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * http tunnel 代理模式下 嵌套socks5处理器
 * <p>
 * 关于channel中的数据类型：
 * 1. 首先通过CONNECT请求建立socks5连接，此时数据类型是HttpRequest
 * 2. CONNECT 连接成功之后移除双方编解码，此时数据类型是ByteBuf
 * （如果增加了ssl解析，那么缓存中的数据类型就不是ByteBuf了）
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public class HttpTunnel2Socks5Handler extends TempAbstractInRelayHandler<HttpObject> {

  private DefaultHttpRequest httpRequest;

  public HttpTunnel2Socks5Handler(DefaultHttpRequest httpRequest) {
    this.httpRequest = httpRequest;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.info("[ {}{}{} ] {} channel active...", ctx.channel(), LOG_MSG, remoteChannelRef.get(), getSimpleName(this));

    Channel clientChannel = ctx.channel();

    Address address = resolveTunnelAddr(httpRequest.uri());
    clientChannel.attr(REQUEST_ADDRESS).set(address);

    Bootstrap remoteBootStrap = new Bootstrap();
    remoteBootStrap.group(clientChannel.eventLoop())
        .channel(Constants.channelClass)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)
        .handler(new HttpOutboundInitializer(this, clientChannel));

    String connHost;
    int connPort;
    if (ClientConfig.HTTP_2_SOCKS5) {
      connHost = ClientConfig.LOCAL_ADDRESS;
      connPort = ClientConfig.SOCKS_LOCAL_PORT;
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
  protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
    Channel remoteChannel = remoteChannelRef.get();
    synchronized (requestTempLists) {
      if (isConnected) {
        ReferenceCountUtil.retain(msg);
        remoteChannel.writeAndFlush(msg);
        logger.debug("[ {}{}{} ] transfer http tunnel request to socks: {}", ctx.channel(), LOG_MSG_OUT, remoteChannel, msg);
      } else {
        if (msg instanceof ByteBuf) {
          ByteBuf byteBuf = (ByteBuf) msg;
          if (byteBuf.readableBytes() <= 0) {
            logger.warn("[ {}{}{} ] discard unreadable msg type: {}", ctx.channel(), LOG_MSG, remoteChannel, msg);
            return;
          }
          ReferenceCountUtil.retain(byteBuf);
          requestTempLists.add(byteBuf);
          logger.debug("[ {}{}{} ] add transfer http tunnel request to temp list: {}", ctx.channel(), LOG_MSG, remoteChannel, msg);
        } else {
          logger.warn("[ {}{}{} ] unhandled msg type: {}", ctx.channel(), LOG_MSG, remoteChannel, msg);
        }
      }
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

}
