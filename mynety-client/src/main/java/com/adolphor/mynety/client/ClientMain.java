package com.adolphor.mynety.client;

import com.adolphor.mynety.client.config.ClientConfig;
import com.adolphor.mynety.client.config.ConfigLoader;
import com.adolphor.mynety.client.http.HttpInboundInitializer;
import com.adolphor.mynety.common.constants.Constants;
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
 * 客户端启动入口
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Slf4j
public final class ClientMain {

  @SuppressWarnings("Duplicates")
  public static void main(String[] args) throws Exception {

    ConfigLoader.loadConfig();
    ClientConfig.checkServers();

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

    new Thread(() -> {
      EventLoopGroup sBossGroup = null;
      EventLoopGroup sWorkerGroup = null;
      try {
        sBossGroup = (EventLoopGroup) Constants.bossGroupClass.getDeclaredConstructor(int.class).newInstance(1);
        sWorkerGroup = (EventLoopGroup) Constants.bossGroupClass.getDeclaredConstructor().newInstance();
        ServerBootstrap sServerBoot = new ServerBootstrap();
        sServerBoot.group(sBossGroup, sWorkerGroup)
            .channel(Constants.serverChannelClass)
            .option(ChannelOption.TCP_NODELAY, true)
            .handler(new LoggingHandler(LogLevel.DEBUG))
            .childHandler(new PipelineInitializer());
        String sLocalHost = ClientConfig.IS_PUBLIC ? Constants.ALL_LOCAL_ADDRESS : Constants.LOOPBACK_ADDRESS;
        ChannelFuture sFuture = sServerBoot.bind(sLocalHost, ClientConfig.SOCKS_PROXY_PORT).sync();
        sFuture.channel().closeFuture().sync();
      } catch (Exception e) {
        logger.error("", e);
      } finally {
        sBossGroup.shutdownGracefully();
        sWorkerGroup.shutdownGracefully();
      }
    }, "socks-proxy-thread").start();

    new Thread(() -> {
      EventLoopGroup hBossGroup = null;
      EventLoopGroup hWorkerGroup = null;
      try {
        hBossGroup = (EventLoopGroup) Constants.bossGroupClass.getDeclaredConstructor(int.class).newInstance(1);
        hWorkerGroup = (EventLoopGroup) Constants.bossGroupClass.getDeclaredConstructor().newInstance();
        ServerBootstrap hServerBoot = new ServerBootstrap();
        hServerBoot.group(hBossGroup, hWorkerGroup)
            .channel(Constants.serverChannelClass)
            .option(ChannelOption.TCP_NODELAY, true)
            .handler(new LoggingHandler(LogLevel.DEBUG))
            .childHandler(new HttpInboundInitializer());
        String hLocalHost = ClientConfig.IS_PUBLIC ? Constants.ALL_LOCAL_ADDRESS : Constants.LOOPBACK_ADDRESS;
        ChannelFuture hFuture = hServerBoot.bind(hLocalHost, ClientConfig.HTTP_PROXY_PORT).sync();
        hFuture.channel().closeFuture().sync();
      } catch (Exception e) {
        logger.error("", e);
      } finally {
        hBossGroup.shutdownGracefully();
        hWorkerGroup.shutdownGracefully();
      }
    }, "http/https-proxy-thread").start();

  }
}
