package com.adolphor.mynety.client.http;

import com.adolphor.mynety.client.adapter.SocksHandsShakeHandler;
import com.adolphor.mynety.client.config.ClientConfig;
import com.adolphor.mynety.common.nettyWrapper.AbstractInRelayHandler;
import com.adolphor.mynety.common.utils.SocksServerUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.LOG_MSG;

/**
 * http 代理模式下 远程连接处理器列表
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public class HttpOutboundInitializer extends ChannelInitializer<SocketChannel> {

  private Channel clientChannel;
  private AbstractInRelayHandler inRelayHandler;

  public HttpOutboundInitializer(AbstractInRelayHandler inRelayHandler, Channel clientChannel) {
    this.clientChannel = clientChannel;
    this.inRelayHandler = inRelayHandler;
  }

  @Override
  @SuppressWarnings("Duplicates")
  protected void initChannel(SocketChannel ch) throws Exception {
    // 如果需要HTTP通过socks5加密通信，那么需要激活socks5代理
    if (ClientConfig.HTTP_2_SOCKS5) {
      ch.pipeline().addLast(new SocksHandsShakeHandler(clientChannel));
      logger.info("[ {}{}{} ] http tunnel out pipeline add handlers: SocksHandsShakeHandler", clientChannel, LOG_MSG, ch);
    }

    // 所有代理都增加 HTTP 编解码类
    ch.pipeline().addLast(new HttpClientCodec());
    logger.info("[ {}{}{} ] http tunnel out pipeline add handlers: HttpClientCodec", clientChannel, LOG_MSG, ch);
    // 个性化协议的个性化处理器（当前HTTP的远程连接处理器可以共用）
    ch.pipeline().addLast(new HttpRemoteHandler(inRelayHandler, clientChannel));
    logger.info("[ {}{}{} ] http tunnel out pipeline add handlers: HttpRemoteHandler", clientChannel, LOG_MSG, ch);

  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    logger.error("[ " + ctx.channel() + LOG_MSG + "] error: ", cause);
    SocksServerUtils.flushOnClose(ctx.channel());
  }

}
