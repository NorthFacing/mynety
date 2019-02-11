package com.adolphor.mynety.client.http;

import com.adolphor.mynety.client.config.ClientConfig;
import com.adolphor.mynety.common.wrapper.AbstractOutBoundHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.ATTR_IN_RELAY_CHANNEL;
import static com.adolphor.mynety.common.constants.Constants.ATTR_IS_HTTP_TUNNEL;
import static com.adolphor.mynety.common.constants.Constants.CONNECTION_ESTABLISHED;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * http 代理模式下 远程处理器，连接目标地址：
 * 1. 如果开启了HTTP2SOCKS，那么连接的是socks服务器
 * 2. 如果没有开启，那么连接的是请求的真实目的地址
 * <p>
 * 本类中不指定数据类型，数据类型和编解码器都由 inRelay 处理器那里指定
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.4
 */
@Slf4j
@ChannelHandler.Sharable
public class HttpOutBoundHandler extends AbstractOutBoundHandler<Object> {

  public static final HttpOutBoundHandler INSTANCE = new HttpOutBoundHandler();

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
    Channel inRelayChannel = ctx.channel().attr(ATTR_IN_RELAY_CHANNEL).get();
    if (inRelayChannel.attr(ATTR_IS_HTTP_TUNNEL).get()) {
      DefaultHttpResponse response = new DefaultHttpResponse(HTTP_1_1, CONNECTION_ESTABLISHED);
      response.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderNames.CONNECTION);
      inRelayChannel.writeAndFlush(response).addListener((ChannelFutureListener) future -> {
        if (future.isSuccess()) {
          removeHttpHandler(ctx, inRelayChannel);
        } else {
          logger.warn(ctx.channel().toString(), future.cause());
          ctx.close();
        }
      });
    } else if (!ClientConfig.HTTP_MITM) {
      removeHttpHandler(ctx, inRelayChannel);
    }
  }

  private void removeHttpHandler(ChannelHandlerContext ctx, Channel inRelayChannel) {
    inRelayChannel.pipeline().remove(HttpObjectAggregator.class);
    inRelayChannel.pipeline().remove(HttpServerCodec.class);

    ctx.channel().pipeline().remove(HttpObjectAggregator.class);
    ctx.channel().pipeline().remove(HttpClientCodec.class);
  }

  /**
   * 接收请求HTTP的回复信息，并转发给客户端返回给用户
   * <p>
   * msg的数据类型有两种：
   * 1. 如果请求是 HTTP 或者 HTTPS 开启了MITM功能，那么就是 FullHttpRequest 类型
   * 2. 否则就是原始的 ByteBuf 数据类型
   *
   * @param ctx
   * @param msg
   */
  @Override
  public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    Channel inRelayChannel = ctx.channel().attr(ATTR_IN_RELAY_CHANNEL).get();
    if (!inRelayChannel.isOpen()) {
      channelClose(ctx);
      return;
    }
    ReferenceCountUtil.retain(msg);
    inRelayChannel.writeAndFlush(msg);
  }

}

