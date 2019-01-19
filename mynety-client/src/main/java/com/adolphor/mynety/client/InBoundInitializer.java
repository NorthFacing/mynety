package com.adolphor.mynety.client;

import com.adolphor.mynety.common.wrapper.AbstractInBoundInitializer;
import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import lombok.extern.slf4j.Slf4j;

import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * 初始化处理器集
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
@ChannelHandler.Sharable
public final class InBoundInitializer extends AbstractInBoundInitializer {

  public static final InBoundInitializer INSTANCE = new InBoundInitializer();

  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    super.initChannel(ch);
    logger.debug("[ {} ]【{}】调用 initChannel 方法开始……", ch, getSimpleName(this));
    // 检测socks版本并初始化对应版本的实例
    ch.pipeline().addLast(new SocksPortUnificationServerHandler());
    logger.info("[ {} ]【InBoundInitializer】增加处理器: SocksPortUnificationServerHandler", ch.id());
    // 消息具体处理类
    ch.pipeline().addLast(AuthHandler.INSTANCE);
    logger.info("[ {} ]【InBoundInitializer】增加处理器: AuthHandler", ch.id());
  }
}

/**
 * Note：
 * SocksPortUnificationServerHandler 中的decode方法中，对于socks5，添加的 Socks5InitialRequestDecoder 实现了 ReplayingDecoder，
 * 可以用一种状态机式的方式解码二进制的请求。状态变为 SUCCESS 以后，就不再解码任何数据。
 */
