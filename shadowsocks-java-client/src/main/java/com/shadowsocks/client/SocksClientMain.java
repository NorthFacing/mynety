package com.shadowsocks.client;

import com.shadowsocks.common.config.ConfigXmlLoader;
import com.shadowsocks.common.config.Constants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public final class SocksClientMain {

	static final int PORT = Integer.parseInt(System.getProperty("port", "1081"));
	static final String HOST = System.getProperty("host", "0.0.0.0");

	private static final String CONFIG = "/Users/adolphor/IdeaProjects/bob/shadowsocks/shadowsocks-java/shadowsocks-java-client/src/main/resources/config.xml";

	public static void main(String[] args) throws Exception {

		ConfigXmlLoader.load(CONFIG, Constants.config);

		EventLoopGroup bossGroup = new NioEventLoopGroup(1);    // 接收请求
		EventLoopGroup workerGroup = new NioEventLoopGroup();             // 处理请求
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)                  // NioServerSocket，ss的客户端，但它是接收请求的服务端
				.handler(new LoggingHandler(LogLevel.DEBUG))            // 日志
				.childHandler(new SocksClientInitializer());            // child的各个handler
			b.bind(HOST, PORT).sync().channel().closeFuture().sync();   // sync：请求进行阻塞处理
		} finally {
			bossGroup.shutdownGracefully();                             // 关闭资源
			workerGroup.shutdownGracefully();
		}

	}
}
