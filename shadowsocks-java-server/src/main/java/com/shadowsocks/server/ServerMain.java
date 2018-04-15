package com.shadowsocks.server;

import com.shadowsocks.server.Config.Config;
import com.shadowsocks.server.Config.ConfigLoader;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.shadowsocks.common.constants.Constants.bossGroup;
import static com.shadowsocks.common.constants.Constants.channelClass;
import static com.shadowsocks.common.constants.Constants.serverChannelClass;
import static com.shadowsocks.common.constants.Constants.workerGroup;

public class ServerMain {

  private static final Logger logger = LoggerFactory.getLogger(ServerMain.class);

  @SuppressWarnings("Duplicates")
  public static void main(String[] args) throws Exception {

    ConfigLoader.loadConfig();

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
          .handler(new LoggingHandler(LogLevel.DEBUG))
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
