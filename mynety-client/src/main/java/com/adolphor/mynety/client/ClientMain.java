package com.adolphor.mynety.client;

import com.adolphor.mynety.client.config.ClientConfig;
import com.adolphor.mynety.client.config.ConfigLoader;
import com.adolphor.mynety.client.http.HttpInboundInitializer;
import com.adolphor.mynety.common.constants.Constants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端启动入口
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Slf4j
public final class ClientMain {

  public static void main(String[] args) throws Exception {

    ConfigLoader.loadConfig();
    ClientConfig.checkServers();

    new Thread(() -> {
      EventLoopGroup sBossGroup = null;
      EventLoopGroup sWorkerGroup = null;
      try {
        sBossGroup = (EventLoopGroup) Constants.bossGroupClass.getDeclaredConstructor().newInstance();
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
        hBossGroup = (EventLoopGroup) Constants.bossGroupClass.getDeclaredConstructor().newInstance();
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
