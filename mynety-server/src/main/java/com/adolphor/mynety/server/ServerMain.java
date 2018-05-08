package com.adolphor.mynety.server;

import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.server.Config.Config;
import com.adolphor.mynety.server.Config.ConfigLoader;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;

/**
 * 服务端启动入口
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Slf4j
public class ServerMain {

  @SuppressWarnings("Duplicates")
  public static void main(String[] args) throws Exception {

    ConfigLoader.loadConfig();

    if (SystemUtils.IS_OS_MAC) { // And BSD system?
      Constants.bossGroupClass = KQueueEventLoopGroup.class;
      Constants.workerGroupClass = KQueueEventLoopGroup.class;
      Constants.serverChannelClass = KQueueServerSocketChannel.class;
      Constants.channelClass = KQueueSocketChannel.class;
    } else if (SystemUtils.IS_OS_LINUX) { // For linux system
      Constants.bossGroupClass = EpollEventLoopGroup.class;
      Constants.workerGroupClass = EpollEventLoopGroup.class;
      Constants.serverChannelClass = EpollServerSocketChannel.class;
      Constants.channelClass = EpollSocketChannel.class;
    } else {
      Constants.bossGroupClass = NioEventLoopGroup.class;
      Constants.workerGroupClass = NioEventLoopGroup.class;
      Constants.serverChannelClass = NioServerSocketChannel.class;
      Constants.channelClass = NioSocketChannel.class;
    }

    EventLoopGroup bossGroup = null;
    EventLoopGroup workerGroup = null;
    try {
      ServerBootstrap serverBoot = new ServerBootstrap();
      bossGroup = (EventLoopGroup) Constants.bossGroupClass.getDeclaredConstructor(int.class).newInstance(1);
      workerGroup = (EventLoopGroup) Constants.bossGroupClass.getDeclaredConstructor().newInstance();
      serverBoot.group(bossGroup, workerGroup)
          .channel(Constants.serverChannelClass)
          .option(ChannelOption.TCP_NODELAY, true)
          .handler(new LoggingHandler(LogLevel.DEBUG))
          .childHandler(new Initializer());
      ChannelFuture future = serverBoot.bind(Config.PROXY_PORT).sync();
      future.channel().closeFuture().sync();
    } catch (Exception e) {
      logger.error("ss服务端启动出错：：", e);
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
