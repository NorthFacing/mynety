package com.shadowsocks.client.socks;

import com.shadowsocks.common.constants.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SocksClientMainTest {

  public static final String HOST = System.getProperty("host", "127.0.0.1");
  public static final int PORT = Integer.parseInt(System.getProperty("port", "1081"));

  public static final String DST_PROTOCOL = System.getProperty("dstProtocol", "https");
  public static final String DST_HOST = System.getProperty("dstHost", "www.baidu.com");
  public static final int DST_PORT = Integer.parseInt(System.getProperty("dstPort", "443"));

  public static void main(String[] args) {
    EventLoopGroup group = new NioEventLoopGroup();
    try {
      Bootstrap b = new Bootstrap();
      b.group(group)
          .channel(NioSocketChannel.class)
          .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
          .option(ChannelOption.TCP_NODELAY, true)
          .handler(new SocksTestInitializer());

      ChannelFuture f = b.connect(HOST, PORT)
          .addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
              log.info(Constants.LOG_MSG + " Socks5 connection success......");
            } else {
              log.error(Constants.LOG_MSG + "Socks5 connection failed......");
            }
          }).sync();

      // Wait until the connection is closed.
      f.channel().closeFuture().sync();
    } catch (Exception e) {
      log.error("", e);
    } finally {
      // Shut down the event loop to terminate all threads.
      group.shutdownGracefully();
    }
  }
}
