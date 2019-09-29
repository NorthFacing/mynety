package com.adolphor.mynety.client.http1;

import com.adolphor.mynety.client.adapter.SocksHandsShakeHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.LOG_LEVEL;
import static com.adolphor.mynety.common.constants.HandlerName.loggingHandler;
import static com.adolphor.mynety.common.constants.HandlerName.socksShakerHandler;

/**
 * http1 代理模式下 远程连接处理器列表
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.4
 */
@Slf4j
@ChannelHandler.Sharable
public class HttpOutBoundInitializer extends ChannelInitializer<SocketChannel> {

  public static final HttpOutBoundInitializer INSTANCE = new HttpOutBoundInitializer();

  @Override
  protected void initChannel(SocketChannel ch) {
    ch.pipeline().addFirst(loggingHandler, new LoggingHandler(LOG_LEVEL));
    ch.pipeline().addAfter(loggingHandler, socksShakerHandler, SocksHandsShakeHandler.INSTANCE);
  }

}
