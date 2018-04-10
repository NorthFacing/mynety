package com.shadowsocks.client;

import com.shadowsocks.client.config.ConfigLoader;
import com.shadowsocks.client.config.ServerConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public final class ClientMain {

	public static void main(String[] args) throws Exception {

		ConfigLoader.loadConfig();

		String localHost;
		if (ServerConfig.PUBLIC) {
			localHost = "0.0.0.0"; // 开放给局域网使用
		} else {
			localHost = "127.0.0.1"; // 仅供本机使用
		}

		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap serverBoot = new ServerBootstrap();
			serverBoot.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.childHandler(new Initializer());

			ChannelFuture future = serverBoot.bind(localHost, ServerConfig.LOCAL_PORT).sync(); //《Netty in Action》: 异步地绑定服务器; 调用sync()方法阻塞等待直到绑定完成
			future.channel().closeFuture().sync();   //《Netty in Action》: 获取 Channel 的 CloseFuture，并且阻塞当前线程直到它完成
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}

	}
}
