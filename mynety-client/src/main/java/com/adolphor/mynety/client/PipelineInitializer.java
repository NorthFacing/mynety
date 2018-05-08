package com.adolphor.mynety.client;

import com.adolphor.mynety.common.constants.Constants;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 初始化处理器集
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Slf4j
public final class PipelineInitializer extends ChannelInitializer<SocketChannel> {

  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    logger.info(" [{}{} ] Init netty handler...", ch, Constants.LOG_MSG);
    ch.pipeline().addLast(new SocksPortUnificationServerHandler()); // 检测socks版本并初始化对应版本的实例
    logger.info("[ {}{} ] add handlers: SocksPortUnificationServerHandler", ch, Constants.LOG_MSG);
    ch.pipeline().addLast(AuthHandler.INSTANCE);             // 消息具体处理类
    logger.info("[ {}{} ] add handlers: AuthHandler", ch, Constants.LOG_MSG);
  }
}

/**
 * Note：
 * SocksPortUnificationServerHandler 中的decode方法中，对于socks5，添加的 Socks5InitialRequestDecoder 实现了 ReplayingDecoder，
 * 可以用一种状态机式的方式解码二进制的请求。状态变为 SUCCESS 以后，就不再解码任何数据。
 */
