package com.adolphor.mynety.client.http.tunnel;

import com.adolphor.mynety.client.config.ClientConfig;
import com.adolphor.mynety.client.http.HttpOutboundInitializer;
import com.adolphor.mynety.common.bean.Address;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.wrapper.AbstractInRelayHandler;
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

import java.util.regex.Matcher;

import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_ADDRESS;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG_OUT;
import static com.adolphor.mynety.common.constants.Constants.LOOPBACK_ADDRESS;
import static com.adolphor.mynety.common.constants.Constants.TUNNEL_ADDR_PATTERN;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * http tunnel 代理模式下 嵌套socks5处理器
 * <p>
 * 关于channel中的数据类型：
 * 1. 首先通过CONNECT请求建立socks5连接，此时数据类型是HttpObject
 * 2. CONNECT 连接成功之后移除双方编解码，此时数据类型是ByteBuf
 * 3. 如果增加了ssl解析，那么就不能移除编解码，缓存中的数据类型就不是ByteBuf了，则还是HttpObject
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public class HttpTunnelConnectionHandler extends AbstractInRelayHandler<Object> {

  private DefaultHttpRequest httpRequest;

  public HttpTunnelConnectionHandler(DefaultHttpRequest httpRequest) {
    this.httpRequest = httpRequest; // 解析出地址，建立socks连接
    requestTempLists.add(httpRequest); // 连接建立之后，转发到目标地址
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.info("[ {}{}{} ] {} channel active...", ctx.channel(), LOG_MSG, remoteChannelRef.get(), getSimpleName(this));

    Channel clientChannel = ctx.channel();

    Address address = resolveTunnelAddr(httpRequest.uri());
    clientChannel.attr(ATTR_REQUEST_ADDRESS).set(address);

    Bootstrap remoteBootStrap = new Bootstrap();
    remoteBootStrap.group(clientChannel.eventLoop())
        .channel(Constants.channelClass)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)
        .handler(new HttpOutboundInitializer(this, clientChannel));

    String connHost;
    int connPort;
    if (ClientConfig.HTTP_2_SOCKS5) {
      connHost = LOOPBACK_ADDRESS;
      connPort = ClientConfig.SOCKS_PROXY_PORT;
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
          logger.debug("[ {}{}{} ] http tunnel connect success: outHost = {}, outPort = {}", clientChannel, LOG_MSG, remoteChannel, connHost, connPort);
        } else {
          logger.debug("[ {}{} ] http tunnel connect failed: outHost = {}, outPort = {}", clientChannel, LOG_MSG, connHost, connPort);
          super.releaseRequestTempLists();
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
    logger.debug("[ {}{}{} ] http tunnel to socks handler receive http msg: {}", ctx.channel(), LOG_MSG, remoteChannelRef.get(), msg);
    Channel remoteChannel = remoteChannelRef.get();
    synchronized (requestTempLists) {
      if (isConnected) {
        ReferenceCountUtil.retain(msg);
        remoteChannel.writeAndFlush(msg);
        logger.debug("[ {}{}{} ] transfer http tunnel request to socks: {}", ctx.channel(), LOG_MSG_OUT, remoteChannel, msg);
      } else {
        if (msg instanceof HttpObject) {
          ReferenceCountUtil.retain(msg);
          requestTempLists.add(msg);
          logger.debug("[ {}{}{} ] add transfer http tunnel request to temp list: {}", ctx.channel(), LOG_MSG, remoteChannel, msg);
        } else {
          logger.warn("[ {}{}{} ] http tunnel unhandled msg type: {}", ctx.channel(), LOG_MSG, remoteChannel, msg);
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
