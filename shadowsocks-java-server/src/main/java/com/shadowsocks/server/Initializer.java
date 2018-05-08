package com.shadowsocks.server;

import com.shadowsocks.common.constants.Constants;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;

/**
 * 初始化处理器集
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Slf4j
public class Initializer extends ChannelInitializer<SocketChannel> {

  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    logger.info("[ {}{} ] Init channels...", ch, Constants.LOG_MSG);
    ch.pipeline().addLast(AddressHandler.INSTANCE);
    logger.info("[ {}{} ] add handler: AddressHandler", ch, LOG_MSG);
  }
}
