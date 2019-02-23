package com.adolphor.mynety.lan;

import com.adolphor.mynety.common.bean.lan.LanMessageDecoder;
import com.adolphor.mynety.common.bean.lan.LanMessageEncoder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.LOG_LEVEL;
import static com.adolphor.mynety.common.constants.HandlerName.heartBeatHandler;
import static com.adolphor.mynety.common.constants.HandlerName.inBoundHandler;
import static com.adolphor.mynety.common.constants.HandlerName.lanMessageDecoder;
import static com.adolphor.mynety.common.constants.HandlerName.lanMessageEncoder;
import static com.adolphor.mynety.common.constants.HandlerName.loggingHandler;


/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
@ChannelHandler.Sharable
public class InBoundInitializer extends ChannelInitializer<SocketChannel> {

  public static final InBoundInitializer INSTANCE = new InBoundInitializer();

  @Override
  @SuppressWarnings("Duplicates")
  protected void initChannel(SocketChannel ch) {
    ch.pipeline().addFirst(loggingHandler, new LoggingHandler(LOG_LEVEL));
    ch.pipeline().addAfter(loggingHandler, lanMessageDecoder, new LanMessageDecoder());
    ch.pipeline().addAfter(lanMessageDecoder, lanMessageEncoder, new LanMessageEncoder());
    ch.pipeline().addAfter(lanMessageEncoder, heartBeatHandler, new HeartBeatHandler());
    ch.pipeline().addAfter(heartBeatHandler, inBoundHandler, InBoundHandler.INSTANCE);
  }
}