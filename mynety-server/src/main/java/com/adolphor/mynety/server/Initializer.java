package com.adolphor.mynety.server;

import com.adolphor.mynety.common.constants.Constants;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * 初始化处理器集
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
public class Initializer extends ChannelInitializer<SocketChannel> {

  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    logger.info("[ {}{} ] Init channels...", ch, Constants.LOG_MSG);
    ch.pipeline().addLast(AddressHandler.INSTANCE);
    logger.info("[ {}{} ] add handler: AddressHandler", ch, Constants.LOG_MSG);
  }
}
