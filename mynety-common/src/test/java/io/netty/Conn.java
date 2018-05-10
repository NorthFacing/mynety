package io.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class Conn {
  public static void main(String[] args) {

    String dstAddr = "baidu.com";
    Integer dstPort = 443;

    Bootstrap bootStrap = new Bootstrap();

    bootStrap.group(new NioEventLoopGroup())
        .channel(NioSocketChannel.class)
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
          }
        });
    try {
      bootStrap.connect(dstAddr, dstPort)
          .addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
              System.out.println("SUCCESS");
            } else {
              System.out.println(future.cause());
            }
          });
    } catch (Exception e) {
      System.out.println(e);
    }
  }
}
