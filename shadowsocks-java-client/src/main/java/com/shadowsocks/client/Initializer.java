package com.shadowsocks.client;

import com.shadowsocks.common.config.Constants;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Initializer extends ChannelInitializer<SocketChannel> {

	private static final Logger logger = LoggerFactory.getLogger(Initializer.class);

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		logger.info(Constants.LOG_MSG + ch);
		ch.pipeline().addLast(new SocksPortUnificationServerHandler()); // 检测socks版本并初始化对应版本的实例
		ch.pipeline().addLast(AuthHandler.INSTANCE);             // 消息具体处理类
	}
}

/**
 * Note：
 * SocksPortUnificationServerHandler 中的decode方法中，对于socks5，添加的 Socks5InitialRequestDecoder 实现了 ReplayingDecoder，
 * 可以用一种状态机式的方式解码二进制的请求。状态变为 SUCCESS 以后，就不再解码任何数据。
 */
