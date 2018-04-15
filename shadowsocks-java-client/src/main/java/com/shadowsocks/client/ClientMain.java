package com.shadowsocks.client;

import com.shadowsocks.client.config.ConfigLoader;
import com.shadowsocks.client.config.ServerConfig;
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
          .childHandler(new Initializer());

      String localHost = ServerConfig.PUBLIC ? "0.0.0.0" : "127.0.0.1";

      ChannelFuture future = serverBoot.bind(localHost, ServerConfig.LOCAL_PORT).sync(); //《Netty in Action》: 异步地绑定服务器; 调用sync()方法阻塞等待直到绑定完成
      future.channel().closeFuture().sync();   //《Netty in Action》: 获取 Channel 的 CloseFuture，并且阻塞当前线程直到它完成
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }

  }
}
