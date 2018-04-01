package com.shadowsocks.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SocksServerInitializer extends ChannelInitializer<SocketChannel> {

	private static final Logger logger = LoggerFactory.getLogger(SocksServerInitializer.class);

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		logger.info("By adolphor: {}", ch);
		ch.pipeline().addLast(new SocksPortUnificationServerHandler()); // 检测socks版本并初始化对应版本的实例
//		ch.pipeline().addLast(SocksClientHandler.INSTANCE);             // 消息具体处理类
	}
}
