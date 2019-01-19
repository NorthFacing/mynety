package com.adolphor.mynety.client.http;

import com.adolphor.mynety.common.utils.ChannelUtils;
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
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG_IN;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * http 代理模式下 远程处理器，连接真正的目标地址
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
    Boolean isHttpTunnel = inRelayChannel.attr(ATTR_IS_HTTP_TUNNEL).get();
    if (isHttpTunnel) {
      DefaultHttpResponse response = new DefaultHttpResponse(HTTP_1_1, CONNECTION_ESTABLISHED);
      response.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderNames.CONNECTION);
      logger.debug("[ {}{}{} ]【{}】httpTunnel 连接成功，发送消息给客户端: {}", inRelayChannel.id(), LOG_MSG, inRelayChannel.id(), getSimpleName(this), response);
      inRelayChannel.writeAndFlush(response).addListener((ChannelFutureListener) future -> {
        if (future.isSuccess()) {
          // 只有在开启MITM的时候，才不移除httpCodec
          boolean isMITM = false;
          boolean isRemoveHttpCodec = !isMITM;
          // 移除 inbound 和 outbound 双方的HTTP编解码 (tunnel代理如果没有增加ssl解析进行MITM，那么就必须移除HTTP编解码器)
          if (isRemoveHttpCodec) {
            logger.debug("[ {}{}{} ]【{}】HTTP代理连接完毕：inRelayChannel 移除处理器: HttpServerCodec", inRelayChannel.id(), LOG_MSG, ctx.channel().id(), getSimpleName(this));
            inRelayChannel.pipeline().remove(HttpServerCodec.class);

            ChannelUtils.loggerHandlers(ctx.channel(), null);
            logger.debug("[ {}{}{} ]【{}】HTTP代理连接完毕：outRelayChannel 移除处理器: HttpObjectAggregator", inRelayChannel.id(), LOG_MSG, ctx.channel().id(), getSimpleName(this));
            ctx.channel().pipeline().remove(HttpObjectAggregator.class);
            logger.debug("[ {}{}{} ]【{}】HTTP代理连接完毕：outRelayChannel 移除处理器: HttpClientCodec", inRelayChannel.id(), LOG_MSG, ctx.channel().id(), getSimpleName(this));
            ctx.channel().pipeline().remove(HttpClientCodec.class);
            ChannelUtils.loggerHandlers(ctx.channel(), null);
          }
        } else {
          ctx.close();
        }
      });
    }

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
  public void channelRead0(ChannelHandlerContext ctx, Object msg) {
    Channel inRelayChannel = ctx.channel().attr(ATTR_IN_RELAY_CHANNEL).get();
    logger.debug("[ {}{}{} ]【{}】收到请求结果内容: {}", inRelayChannel.id(), LOG_MSG_IN, ctx.channel().id(), getSimpleName(this), msg);
    if (!inRelayChannel.isOpen()) {
      channelClose(ctx);
      return;
    }
    try {
      ReferenceCountUtil.retain(msg);
      inRelayChannel.writeAndFlush(msg);
      logger.debug("[ {}{}{} ]【{}】】收到请求结果发送给客户端: {}", inRelayChannel.id(), LOG_MSG_IN, ctx.channel().id(), getSimpleName(this), msg);
    } catch (Exception e) {
      logger.error("[ " + inRelayChannel.id() + LOG_MSG_IN + ctx.channel().id() + " ] error", e);
      channelClose(ctx);
    }
  }

}

