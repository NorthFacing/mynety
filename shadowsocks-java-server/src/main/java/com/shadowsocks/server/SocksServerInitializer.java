package com.shadowsocks.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socks.SocksInitRequestDecoder;
import io.netty.handler.codec.socks.SocksMessageEncoder;

import java.util.Map;

public class SocksServerInitializer extends ChannelInitializer<SocketChannel> {

	private SocksMessageEncoder socksMessageEncoder;
	private SocksServerHandler socksServerHandler;

	public SocksServerInitializer(Map<String, String> config) {
		socksMessageEncoder = new SocksMessageEncoder();
		socksServerHandler = new SocksServerHandler(config);
	}

	@Override
	protected void initChannel(SocketChannel socketChannel) throws Exception {
		ChannelPipeline pipeline = socketChannel.pipeline();
		pipeline.addLast(new SocksInitRequestDecoder());
		pipeline.addLast(socksMessageEncoder);
		pipeline.addLast(socksServerHandler);
	}

}
