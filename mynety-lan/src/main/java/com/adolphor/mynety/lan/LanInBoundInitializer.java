package com.adolphor.mynety.lan;

import com.adolphor.mynety.common.bean.lan.LanMessageDecoder;
import com.adolphor.mynety.common.bean.lan.LanMessageEncoder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
@ChannelHandler.Sharable
public class LanInBoundInitializer extends ChannelInitializer<SocketChannel> {

  public static final LanInBoundInitializer INSTANCE = new LanInBoundInitializer();

  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    logger.debug("[ {} ]【LanInBoundInitializer】调用 initChannel 方法开始……", ch);
    ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
    logger.info(" [ {} ]【LanInBoundInitializer】增加处理器: LoggingHandler", ch.id());
    ch.pipeline().addLast(new LanMessageDecoder());
    logger.info(" [ {} ]【LanInBoundInitializer】增加处理器: LanMessageDecoder", ch.id());
    ch.pipeline().addLast(new LanMessageEncoder());
    logger.info(" [ {} ]【LanInBoundInitializer】增加处理器: LanMessageEncoder", ch.id());
    ch.pipeline().addLast(new HeartBeatHandler());
    logger.info(" [ {} ]【LanInBoundInitializer】增加处理器: HeartBeatHandler", ch.id());
    ch.pipeline().addLast(LanInBoundHandler.INSTANCE);
    logger.info(" [ {} ]【LanInBoundInitializer】增加处理器: LanInBoundHandler", ch.id());
  }
}