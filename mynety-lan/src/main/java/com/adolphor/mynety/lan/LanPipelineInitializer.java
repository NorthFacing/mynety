package com.adolphor.mynety.lan;

import com.adolphor.mynety.common.bean.lan.LanMessageDecoder;
import com.adolphor.mynety.common.bean.lan.LanMessageEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.LOG_MSG;

/**
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.5
 */
@Slf4j
public class LanPipelineInitializer extends ChannelInitializer<SocketChannel> {

  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    logger.info(" [{}{} ] Lan init handler...", ch, LOG_MSG);
    ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
    logger.info("[ {}{} ] add handlers: LoggingHandler", ch, LOG_MSG);
    ch.pipeline().addLast(new LanMessageDecoder());
    logger.info("[ {}{} ] add handlers: LanMessageDecoder", ch, LOG_MSG);
    ch.pipeline().addLast(new LanMessageEncoder());
    logger.info("[ {}{} ] add handlers: LanMessageEncoder", ch, LOG_MSG);
    ch.pipeline().addLast(new HeartBeatHandler());
    logger.info("[ {}{} ] add handlers: HeartBeatHandler", ch, LOG_MSG);
    ch.pipeline().addLast(new LanConnectionHandler());
    logger.info("[ {}{} ] add handlers: LanConnectionHandler", ch, LOG_MSG);
  }
}