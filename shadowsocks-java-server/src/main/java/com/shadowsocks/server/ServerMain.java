package com.shadowsocks.server;

import com.shadowsocks.server.Config.Config;
import com.shadowsocks.server.Config.ConfigLoader;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerMain {

	private static final Logger logger = LoggerFactory.getLogger(ServerMain.class);

	public static void main(String[] args) throws Exception {

		ConfigLoader.loadConfig();

		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap serverBoot = new ServerBootstrap();
			serverBoot.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.handler(new LoggingHandler(LogLevel.DEBUG))
				.option(ChannelOption.SO_KEEPALIVE, true)
				.childHandler(new Initializer());
			ChannelFuture future = serverBoot.bind(Config.LOCAL_PORT).sync();
			future.channel().closeFuture().sync();
		} catch (Exception e) {
			logger.error("ss服务端启动出错：：", e);
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
}
