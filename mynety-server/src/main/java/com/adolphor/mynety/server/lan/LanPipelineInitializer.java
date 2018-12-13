package com.adolphor.mynety.server.lan;

import com.adolphor.mynety.common.bean.lan.LanMessageDecoder;
import com.adolphor.mynety.common.bean.lan.LanMessageEncoder;
import com.adolphor.mynety.common.constants.Constants;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
public class LanPipelineInitializer extends ChannelInitializer<SocketChannel> {

  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    logger.info(" [{}{} ] Lan init handler...", ch, Constants.LOG_MSG);
    ch.pipeline().addLast(new LanMessageDecoder());
    logger.info("[ {}{} ] add handlers: LanMessageDecoder", ch, Constants.LOG_MSG);
    ch.pipeline().addLast(new LanMessageEncoder());
    logger.info("[ {}{} ] add handlers: LanMessageEncoder", ch, Constants.LOG_MSG);
    ch.pipeline().addLast(new HeartBeatHandler());
    logger.info("[ {}{} ] add handlers: HeartBeatHandler", ch, Constants.LOG_MSG);
    ch.pipeline().addLast(new LanConnectionHandler());
    logger.info("[ {}{} ] add handlers: LanConnectionHandler", ch, Constants.LOG_MSG);
  }
}