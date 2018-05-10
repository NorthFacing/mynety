package com.adolphor.mynety.server;

import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.server.config.Config;
import com.adolphor.mynety.server.config.ConfigLoader;
import com.adolphor.mynety.server.lan.LanPipelineInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 服务端启动入口
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Slf4j
public class ServerMain {

  public static void main(String[] args) throws Exception {

    ConfigLoader.loadConfig();

    new Thread(() -> {
      EventLoopGroup bossGroup = null;
      EventLoopGroup workerGroup = null;
      try {
        ServerBootstrap serverBoot = new ServerBootstrap();
        bossGroup = (EventLoopGroup) Constants.bossGroupClass.getDeclaredConstructor().newInstance();
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
    }, "socks-proxy-thread").start();

    new Thread(() -> {
      EventLoopGroup bossGroup = null;
      EventLoopGroup workerGroup = null;
      try {
        ServerBootstrap serverBoot = new ServerBootstrap();
        bossGroup = (EventLoopGroup) Constants.bossGroupClass.getDeclaredConstructor().newInstance();
        workerGroup = (EventLoopGroup) Constants.bossGroupClass.getDeclaredConstructor().newInstance();
        serverBoot.group(bossGroup, workerGroup)
            .channel(Constants.serverChannelClass)
            .option(ChannelOption.TCP_NODELAY, true)
            .handler(new LoggingHandler(LogLevel.DEBUG))
            .childHandler(new LanPipelineInitializer());
        ChannelFuture future = serverBoot.bind(Config.LAN_SERVER_PORT).sync();
        future.channel().closeFuture().sync();
      } catch (Exception e) {
        logger.error("LAN服务端启动出错：：", e);
      } finally {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
      }
    }, "socks-lan-thread").start();

  }
}
