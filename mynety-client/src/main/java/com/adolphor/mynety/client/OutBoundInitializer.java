package com.adolphor.mynety.client;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * OutBound 初始化处理器集
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
@ChannelHandler.Sharable
public class OutBoundInitializer extends ChannelInitializer<SocketChannel> {

  public static final OutBoundInitializer INSTANCE = new OutBoundInitializer();

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    logger.debug("[ {} ]【{}】调用 initChannel 方法开始……", ch, getSimpleName(this));
    ch.pipeline().addLast(OutBoundHandler.INSTANCE);
    logger.info("[ {} ]【{}】增加处理器: OutBoundHandler", ch.id(), getSimpleName(this));
  }
}
