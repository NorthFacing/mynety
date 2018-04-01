package com.shadowsocks.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public final class SocksServerMain {

	static final int PORT = Integer.parseInt(System.getProperty("port", "1082"));

	public static void main(String[] args) throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);    // 接收请求
		EventLoopGroup workerGroup = new NioEventLoopGroup();             // 处理请求
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)                  // NioServerSocket，ss的客户端，但它是接收请求的服务端
				.handler(new LoggingHandler(LogLevel.DEBUG))             // 日志
				.childHandler(new SocksServerInitializer());            // child的各个handler
			b.bind(PORT).sync().channel().closeFuture().sync();         // sync：请求进行阻塞处理
		} finally {
			bossGroup.shutdownGracefully();                             // 关闭资源
			workerGroup.shutdownGracefully();
		}
	}
}
