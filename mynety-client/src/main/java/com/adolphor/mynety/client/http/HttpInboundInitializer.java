package com.adolphor.mynety.client.http;

import com.adolphor.mynety.common.utils.SocksServerUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.LOG_MSG;

/**
 * http 代理入口 处理器列表
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public class HttpInboundInitializer extends ChannelInitializer<SocketChannel> {

  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    logger.info("[ {}{} ] Init http handler...", ch, LOG_MSG);
    ch.pipeline().addLast(new HttpServerCodec());
    logger.info("[ {}{} ] add handlers: HttpServerCodec", ch, LOG_MSG);
    ch.pipeline().addLast(new HttpProxyHandler());
    logger.info("[ {}{} ] add handlers: HttpProxyHandler", ch, LOG_MSG);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    logger.error("[ " + ctx.channel() + LOG_MSG + "] HttpInboundInitializer error: ", cause);
    SocksServerUtils.flushOnClose(ctx.channel());
  }

}