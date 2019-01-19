package com.adolphor.mynety.client.http;

import com.adolphor.mynety.common.utils.ChannelUtils;
import com.adolphor.mynety.common.wrapper.AbstractInBoundInitializer;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.LOG_MSG;

/**
 * http 代理入口 处理器列表
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.4
 */
@Slf4j
@ChannelHandler.Sharable
public class HttpInBoundInitializer extends AbstractInBoundInitializer {

  public static final HttpInBoundInitializer INSTANCE = new HttpInBoundInitializer();

  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    super.initChannel(ch);
    // 所有代理都增加 HTTP 编解码类，才能解析出来HTTP请求地址等信息
    ch.pipeline().addLast(new HttpServerCodec());
    logger.info("[ {} ]【HttpInBoundInitializer】增加处理器: HttpServerCodec", ch.id());
    ch.pipeline().addLast(new HttpObjectAggregator(65536));
    logger.info("[ {} ]【HttpInBoundInitializer】增加处理器: HttpObjectAggregator", ch.id());
    ch.pipeline().addLast(HttpProxyHandler.INSTANCE);
    logger.info("[ {} ]【HttpInBoundInitializer】增加处理器: HttpProxyHandler", ch.id());
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    logger.error("[ " + ctx.channel().id() + LOG_MSG + "] HttpInBoundInitializer error: ", cause);
    ChannelUtils.closeOnFlush(ctx.channel());
  }

}