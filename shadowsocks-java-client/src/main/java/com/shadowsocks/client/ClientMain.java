package com.shadowsocks.client;

import com.shadowsocks.client.config.ConfigLoader;
import com.shadowsocks.client.config.ServerConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.commons.lang3.SystemUtils;

import static com.shadowsocks.common.constants.Constants.bossGroup;
import static com.shadowsocks.common.constants.Constants.channelClass;
import static com.shadowsocks.common.constants.Constants.serverChannelClass;
import static com.shadowsocks.common.constants.Constants.workerGroup;

public final class ClientMain {


  @SuppressWarnings("Duplicates")
  public static void main(String[] args) throws Exception {

    ConfigLoader.loadConfig();
    ServerConfig.checkServers();

    if (SystemUtils.IS_OS_MAC) { // And BSD system?
      bossGroup = new KQueueEventLoopGroup(1);
      workerGroup = new KQueueEventLoopGroup();
      serverChannelClass = KQueueServerSocketChannel.class;
      channelClass = KQueueSocketChannel.class;
    } else if (SystemUtils.IS_OS_LINUX) { // For linux system
      bossGroup = new EpollEventLoopGroup(1);
      workerGroup = new EpollEventLoopGroup();
      serverChannelClass = EpollServerSocketChannel.class;
      channelClass = EpollSocketChannel.class;
    } else {
      bossGroup = new NioEventLoopGroup(1);
      workerGroup = new NioEventLoopGroup();
      serverChannelClass = NioServerSocketChannel.class;
      channelClass = NioSocketChannel.class;
    }

    try {
      ServerBootstrap serverBoot = new ServerBootstrap();
      serverBoot.group(bossGroup, workerGroup)
          .channel(serverChannelClass)
          .option(ChannelOption.TCP_NODELAY, true)
          .handler(new LoggingHandler(LogLevel.DEBUG))
          .childHandler(new Initializer());

      String localHost = ServerConfig.PUBLIC ? "0.0.0.0" : "127.0.0.1";

      ChannelFuture future = serverBoot.bind(localHost, ServerConfig.LOCAL_PORT).sync();
      future.channel().closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }

  }
}
