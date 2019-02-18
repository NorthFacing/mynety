package com.adolphor.mynety.client.http;

import com.adolphor.mynety.common.wrapper.AbstractOutBoundHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.ATTR_IN_RELAY_CHANNEL;
import static com.adolphor.mynety.common.constants.Constants.ATTR_IS_HTTP_TUNNEL;
import static com.adolphor.mynety.common.constants.Constants.CONNECTION_ESTABLISHED;
import static com.adolphor.mynety.common.constants.HandlerName.httpAggregatorHandler;
import static com.adolphor.mynety.common.constants.HandlerName.httpClientCodec;
import static com.adolphor.mynety.common.constants.HandlerName.httpServerCodec;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * the outBound address could be :
 * 1. socks client
 * 2. destination address
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.4
 */
@Slf4j
@ChannelHandler.Sharable
public class HttpOutBoundHandler extends AbstractOutBoundHandler<Object> {

  public static final HttpOutBoundHandler INSTANCE = new HttpOutBoundHandler();

  private static final DefaultHttpResponse response = new DefaultHttpResponse(HTTP_1_1, CONNECTION_ESTABLISHED);

  /**
   * remove all handlers after
   *
   * @param ctx
   * @throws Exception
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
    Channel inRelayChannel = ctx.channel().attr(ATTR_IN_RELAY_CHANNEL).get();
    if (inRelayChannel.attr(ATTR_IS_HTTP_TUNNEL).get()) {
      response.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderNames.CONNECTION);
      inRelayChannel.writeAndFlush(response);
    }

    inRelayChannel.pipeline().remove(httpAggregatorHandler);
    inRelayChannel.pipeline().remove(httpServerCodec);

    ctx.channel().pipeline().remove(httpAggregatorHandler);
    ctx.channel().pipeline().remove(httpClientCodec);
  }

}

