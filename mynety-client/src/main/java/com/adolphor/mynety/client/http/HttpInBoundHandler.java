package com.adolphor.mynety.client.http;

import com.adolphor.mynety.client.config.ClientConfig;
import com.adolphor.mynety.client.utils.cert.CertPool;
import com.adolphor.mynety.common.bean.Address;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.utils.ChannelUtils;
import com.adolphor.mynety.common.wrapper.AbstractInBoundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

import static com.adolphor.mynety.client.config.ClientConfig.HTTPS_CERT_CONFIG;
import static com.adolphor.mynety.common.constants.Constants.ATTR_IN_RELAY_CHANNEL;
import static com.adolphor.mynety.common.constants.Constants.ATTR_OUT_RELAY_CHANNEL_REF;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_ADDRESS;
import static com.adolphor.mynety.common.constants.Constants.CONNECT_TIMEOUT;
import static com.adolphor.mynety.common.constants.Constants.LOOPBACK_ADDRESS;

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
    Address address = ctx.channel().attr(ATTR_REQUEST_ADDRESS).get();
    Bootstrap remoteBootStrap = new Bootstrap();
    remoteBootStrap.group(ctx.channel().eventLoop())
        .channel(Constants.channelClass)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT)
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

    remoteBootStrap.connect(connHost, connPort).addListener((ChannelFutureListener) future -> {
      if (future.isSuccess()) {
        Channel outRelayChannel = future.channel();
        ctx.channel().attr(ATTR_OUT_RELAY_CHANNEL_REF).get().set(outRelayChannel);
        outRelayChannel.attr(ATTR_IN_RELAY_CHANNEL).set(ctx.channel());
      } else {
        logger.warn(ctx.channel().toString(), future.cause());
        channelClose(ctx);
      }
    });
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    Channel outRelayChannel = ctx.channel().attr(ATTR_OUT_RELAY_CHANNEL_REF).get().get();
    if (ClientConfig.HTTP_MITM && msg instanceof ByteBuf) {
      ByteBuf bufMsg = (ByteBuf) msg;
      // TODO 如果开启MITM，在使用postman测试的时候就会出现问题
      if (bufMsg.getByte(0) == 22) { //ssl握手
        Address address = ctx.channel().attr(ATTR_REQUEST_ADDRESS).get();
        int port = ((InetSocketAddress) ctx.channel().localAddress()).getPort();
        SslContext sslCtx = SslContextBuilder.forServer(HTTPS_CERT_CONFIG.getMitmPriKey(), CertPool.getCert(address.getHost(), HTTPS_CERT_CONFIG)).build();
        ctx.pipeline().addFirst("sslHandler", sslCtx.newHandler(ctx.alloc()));
        ctx.pipeline().addAfter("sslHandler", "httpCodec", new HttpServerCodec());
        ctx.pipeline().addAfter("httpCodec", "httpAggregator", new HttpObjectAggregator(6553600));
        ChannelUtils.loggerHandlers(ctx.channel(), null);

        outRelayChannel.pipeline().addFirst("sslHandler", HTTPS_CERT_CONFIG.getClientSslCtx().newHandler(outRelayChannel.alloc(), address.getHost(), address.getPort()));
        outRelayChannel.pipeline().addAfter("sslHandler", "httpCodec", new HttpClientCodec());
        outRelayChannel.pipeline().addAfter("httpCodec", "httpAggregator", new HttpObjectAggregator(6553600));
        ChannelUtils.loggerHandlers(outRelayChannel, null);
        // repass the pipeline, to get the https MITM msg
        ReferenceCountUtil.retain(msg);
        ctx.pipeline().fireChannelRead(msg);
        return;
      }
    }
    ReferenceCountUtil.retain(msg);
    outRelayChannel.writeAndFlush(msg);
  }

}
