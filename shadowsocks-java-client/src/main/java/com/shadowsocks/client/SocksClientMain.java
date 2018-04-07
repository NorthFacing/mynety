package com.shadowsocks.client;

import com.shadowsocks.client.config.ClientConfig;
import com.shadowsocks.client.config.ConfigXmlLoader;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public final class SocksClientMain {

	private static final String CONFIG = "client-config.xml";

		public static void main(String[] args) throws Exception {

		ConfigXmlLoader.loadClient(CONFIG);

		EventLoopGroup bossGroup = new NioEventLoopGroup(1);    // 接收请求
		EventLoopGroup workerGroup = new NioEventLoopGroup();             // 处理请求
		try {
			ServerBootstrap serverBoot = new ServerBootstrap();
			serverBoot.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)                  // NioServerSocket，ss的客户端，接收用户连接请求
				.handler(new LoggingHandler(LogLevel.DEBUG))            // 日志
				.childHandler(new SocksClientInitializer());            // child的各个handler
			ChannelFuture future = serverBoot.bind(ClientConfig.LOCAL_HOST,
				ClientConfig.LOCAL_PORT).sync(); //《Netty in Action》: 异步地绑定服务器; 调用sync()方法阻塞等待直到绑定完成
			future.channel().closeFuture().sync();   //《Netty in Action》: 获取 Channel 的 CloseFuture，并且阻塞当前线程直到它完成
		} finally {
			bossGroup.shutdownGracefully(); //《Netty in Action》: 关闭 EventLoopGroup， 释放所有的资源，包括所有被创建的线程
			workerGroup.shutdownGracefully();
		}

	}
}
