package com.adolphor.mynety.client.http;

import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.wrapper.AbstractInBoundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.client.config.Config.SOCKS_PROXY_PORT;
import static com.adolphor.mynety.common.constants.Constants.ATTR_IN_RELAY_CHANNEL;
import static com.adolphor.mynety.common.constants.Constants.ATTR_OUT_RELAY_CHANNEL_REF;
import static com.adolphor.mynety.common.constants.Constants.CONNECT_TIMEOUT;
import static com.adolphor.mynety.common.constants.Constants.LOOPBACK_ADDRESS;

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
    Bootstrap remoteBootStrap = new Bootstrap();
    remoteBootStrap.group(ctx.channel().eventLoop())
        .channel(Constants.channelClass)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT)
        .handler(HttpOutBoundInitializer.INSTANCE);

    remoteBootStrap.connect(LOOPBACK_ADDRESS, SOCKS_PROXY_PORT).addListener((ChannelFutureListener) future -> {
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
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
    Channel outRelayChannel = ctx.channel().attr(ATTR_OUT_RELAY_CHANNEL_REF).get().get();
    ReferenceCountUtil.retain(msg);
    outRelayChannel.writeAndFlush(msg);
  }

}
