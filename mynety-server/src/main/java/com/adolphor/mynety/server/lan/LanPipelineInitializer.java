package com.adolphor.mynety.server.lan;

import com.adolphor.mynety.common.bean.lan.LanMessageDecoder;
import com.adolphor.mynety.common.bean.lan.LanMessageEncoder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
@ChannelHandler.Sharable
public class LanPipelineInitializer extends ChannelInitializer<SocketChannel> {

  public static final LanPipelineInitializer INSTANCE = new LanPipelineInitializer();

  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    logger.info("[ {} ]【LanPipelineInitializer】开始激活……", ch);
    ch.pipeline().addLast(new LanMessageDecoder());
    logger.info("[ {} ]【LanPipelineInitializer】增加处理器: LanMessageDecoder", ch.id());
    ch.pipeline().addLast(new LanMessageEncoder());
    logger.info("[ {} ]【LanPipelineInitializer】增加处理器: LanMessageEncoder", ch.id());
    ch.pipeline().addLast(new HeartBeatHandler());
    logger.info("[ {} ]【LanPipelineInitializer】增加处理器: HeartBeatHandler", ch.id());
    ch.pipeline().addLast(LanConnInBoundHandler.INSTANCE);
    logger.info("[ {} ]【LanPipelineInitializer】增加处理器: LanConnInBoundHandler", ch.id());
  }
}