package com.adolphor.mynety.client;

import com.adolphor.mynety.client.config.Config;
import com.adolphor.mynety.client.config.ConfigLoader;
import com.adolphor.mynety.client.http.HttpInBoundInitializer;
import com.adolphor.mynety.client.utils.NetUtils;
import com.adolphor.mynety.common.constants.Constants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.client.config.Config.HTTP_PROXY_PORT;
import static com.adolphor.mynety.client.config.Config.SOCKS_PROXY_PORT;
import static com.adolphor.mynety.common.constants.Constants.LOG_LEVEL;

/**
 * entrance of client
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
public final class ClientMain {

  public static void main(String[] args) throws Exception {

    ConfigLoader.loadConfig();
    NetUtils.checkServers();

    String bindHost = Config.IS_PUBLIC ? Constants.ALL_LOCAL_ADDRESS : Constants.LOOPBACK_ADDRESS;

    new Thread(() -> {
      EventLoopGroup sBossGroup = null;
      EventLoopGroup sWorkerGroup = null;
      try {
        sBossGroup = (EventLoopGroup) Constants.bossGroupType.newInstance();
        sWorkerGroup = (EventLoopGroup) Constants.bossGroupType.newInstance();
        ServerBootstrap sServerBoot = new ServerBootstrap();
        sServerBoot.group(sBossGroup, sWorkerGroup)
            .channel(Constants.serverChannelClass)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(InBoundInitializer.INSTANCE);
        ChannelFuture future = sServerBoot.bind(bindHost, SOCKS_PROXY_PORT).sync();
        future.channel().closeFuture().sync();
      } catch (Exception e) {
        logger.error("socks proxy start error: ", e);
      } finally {
        sBossGroup.shutdownGracefully();
        sWorkerGroup.shutdownGracefully();
      }
    }, "socks-proxy-thread").start();

    new Thread(() -> {
      EventLoopGroup hBossGroup = null;
      EventLoopGroup hWorkerGroup = null;
      try {
        hBossGroup = (EventLoopGroup) Constants.bossGroupType.newInstance();
        hWorkerGroup = (EventLoopGroup) Constants.bossGroupType.newInstance();
        ServerBootstrap hServerBoot = new ServerBootstrap();
        hServerBoot.group(hBossGroup, hWorkerGroup)
            .channel(Constants.serverChannelClass)
            .handler(new LoggingHandler(LOG_LEVEL))
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(HttpInBoundInitializer.INSTANCE);
        ChannelFuture future = hServerBoot.bind(bindHost, HTTP_PROXY_PORT).sync();
        future.channel().closeFuture().sync();
      } catch (Exception e) {
        logger.error("http proxy start error: ", e);
      } finally {
        hBossGroup.shutdownGracefully();
        hWorkerGroup.shutdownGracefully();
      }
    }, "http/https-proxy-thread").start();

  }
}
