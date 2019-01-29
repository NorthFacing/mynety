package com.adolphor.mynety.client.http;

import com.adolphor.mynety.client.config.ClientConfig;
import com.adolphor.mynety.common.bean.Address;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.wrapper.AbstractInBoundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

import static com.adolphor.mynety.common.constants.Constants.ATTR_IN_RELAY_CHANNEL;
import static com.adolphor.mynety.common.constants.Constants.ATTR_OUT_RELAY_CHANNEL_REF;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_ADDRESS;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG_OUT;
import static com.adolphor.mynety.common.constants.Constants.LOOPBACK_ADDRESS;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * http 代理模式下 嵌套socks5处理器
 * <p>
 * 关于channel中的数据类型：
 * 1. 首先通过CONNECT请求建立socks5连接，此时数据类型是HttpObject
 * 2. CONNECT 连接成功之后移除双方编解码，此时数据类型是ByteBuf
 * 3. 如果增加了ssl解析，那么就不能移除编解码，缓存中的数据类型就不是ByteBuf了，则还是HttpObject
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.4
 */
@Slf4j
@ChannelHandler.Sharable
public class HttpInBoundHandler extends AbstractInBoundHandler<Object> {

  public static final HttpInBoundHandler INSTANCE = new HttpInBoundHandler();

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
    logger.info("[ {}{} ]【{}】channel active...", ctx.channel().id(), LOG_MSG, getSimpleName(this));
    Address address = ctx.channel().attr(ATTR_REQUEST_ADDRESS).get();
    Bootstrap remoteBootStrap = new Bootstrap();
    remoteBootStrap.group(ctx.channel().eventLoop())
        .channel(Constants.channelClass)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)
        .handler(HttpOutBoundInitializer.INSTANCE);
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
          Channel outRelayChannel = future.channel();
          ctx.channel().attr(ATTR_OUT_RELAY_CHANNEL_REF).get().set(outRelayChannel);
          outRelayChannel.attr(ATTR_IN_RELAY_CHANNEL).set(ctx.channel());
          logger.debug("[ {}{}{} ]【{}】远程连接成功 => {}:{}", ctx.channel().id(), LOG_MSG, outRelayChannel.id(), getSimpleName(this), connHost, connPort);
        } else {
          logger.warn("[ {} ]【{}】远程连接失败 => {}:{}", ctx.channel().id(), getSimpleName(this), connHost, connPort);
          logger.warn(ctx.channel().toString(), future.cause());
          future.cancel(true);
          channelClose(ctx);
        }
      });
    } catch (Exception e) {
      logger.error("[ " + ctx.channel() + LOG_MSG + connHost + ":" + connPort + " ] http1.1 connect internet error", e);
      channelClose(ctx);
    }

  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    logger.debug("[ {}{} ]【{}】收到客户端消息: {}", ctx.channel().id(), LOG_MSG, getSimpleName(this), msg);
    AtomicReference<Channel> outRelayChannelRef = ctx.channel().attr(ATTR_OUT_RELAY_CHANNEL_REF).get();
    ReferenceCountUtil.retain(msg);
    outRelayChannelRef.get().writeAndFlush(msg);
    logger.debug("[ {}{}{} ]【{}】发送消息到{}: {}", ctx.channel().id(), LOG_MSG_OUT, outRelayChannelRef.get().id(), getSimpleName(this), ClientConfig.HTTP_2_SOCKS5 ? "socks代理" : "目的地址", msg);
  }

}
