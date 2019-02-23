package com.adolphor.mynety.client.http;

import com.adolphor.mynety.client.config.Config;
import com.adolphor.mynety.client.utils.cert.CertPool;
import com.adolphor.mynety.common.bean.Address;
import com.adolphor.mynety.common.constants.Constants;
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

import static com.adolphor.mynety.client.config.Config.HTTPS_CERT_CONFIG;
import static com.adolphor.mynety.common.constants.Constants.ATTR_IN_RELAY_CHANNEL;
import static com.adolphor.mynety.common.constants.Constants.ATTR_OUT_RELAY_CHANNEL_REF;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_ADDRESS;
import static com.adolphor.mynety.common.constants.Constants.CONNECT_TIMEOUT;
import static com.adolphor.mynety.common.constants.Constants.LOOPBACK_ADDRESS;
import static com.adolphor.mynety.common.constants.Constants.MAX_CONTENT_LENGTH;
import static com.adolphor.mynety.common.constants.HandlerName.httpAggregatorHandler;
import static com.adolphor.mynety.common.constants.HandlerName.httpClientCodec;
import static com.adolphor.mynety.common.constants.HandlerName.httpServerCodec;
import static com.adolphor.mynety.common.constants.HandlerName.sslClientHandler;
import static com.adolphor.mynety.common.constants.HandlerName.sslServerHandler;

/**
 * http over socks5
 * <p>
 * the msg type of channel:
 * 1. the first request msg is HttpObject (request for building socks5 connection)
 * 2. if open MITM, the msg type is HttpObject
 * 3. if not, the msg type is ByteBuf
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.4
 */
@Slf4j
@ChannelHandler.Sharable
public class HttpInBoundHandler extends AbstractInBoundHandler<Object> {

  public static final HttpInBoundHandler INSTANCE = new HttpInBoundHandler();

  /**
   * build the remote connection:
   * 1. if open http2socks, the remote address is the address listened by socks5 client
   * 2. if not, the remote address is the address requested by USER
   *
   * @param ctx
   * @throws Exception
   */
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
    if (Config.HTTP_2_SOCKS5) {
      connHost = LOOPBACK_ADDRESS;
      connPort = Config.SOCKS_PROXY_PORT;
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
    if (Config.HTTP_MITM && msg instanceof ByteBuf) {
      ByteBuf bufMsg = (ByteBuf) msg;
      // TODO 如果开启MITM，在使用postman测试的时候就会出现问题
      if (bufMsg.getByte(0) == 22) {
        Address address = ctx.channel().attr(ATTR_REQUEST_ADDRESS).get();
        SslContext sslCtx = SslContextBuilder.forServer(HTTPS_CERT_CONFIG.getMitmPriKey(), CertPool.getCert(address.getHost(), HTTPS_CERT_CONFIG)).build();
        ctx.pipeline().addFirst(sslServerHandler, sslCtx.newHandler(ctx.alloc()));
        ctx.pipeline().addAfter(sslServerHandler, httpServerCodec, new HttpServerCodec());
        ctx.pipeline().addAfter(httpServerCodec, httpAggregatorHandler, new HttpObjectAggregator(MAX_CONTENT_LENGTH));
        outRelayChannel.pipeline().addFirst(sslClientHandler, HTTPS_CERT_CONFIG.getClientSslCtx().newHandler(outRelayChannel.alloc(), address.getHost(), address.getPort()));
        outRelayChannel.pipeline().addAfter(sslClientHandler, httpClientCodec, new HttpClientCodec());
        outRelayChannel.pipeline().addAfter(httpClientCodec, httpAggregatorHandler, new HttpObjectAggregator(MAX_CONTENT_LENGTH));
        // re-pass the pipeline, to get the https MITM msg
        ReferenceCountUtil.retain(msg);
        ctx.pipeline().fireChannelRead(msg);
        return;
      }
    }
    ReferenceCountUtil.retain(msg);
    outRelayChannel.writeAndFlush(msg);
  }

}
