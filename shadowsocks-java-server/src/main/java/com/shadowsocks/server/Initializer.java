package com.shadowsocks.server;

import com.shadowsocks.common.config.Constants;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Initializer extends ChannelInitializer<SocketChannel> {

	private static final Logger logger = LoggerFactory.getLogger(Initializer.class);

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		logger.info(Constants.LOG_MSG + ch + " Init channels...");
		ch.pipeline().addLast(AddressHandler.INSTANCE);
	}
}
