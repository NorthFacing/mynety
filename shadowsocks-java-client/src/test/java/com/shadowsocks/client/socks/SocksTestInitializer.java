package com.shadowsocks.client.socks;

import com.shadowsocks.common.constants.Constants;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SocksTestInitializer extends ChannelInitializer<SocketChannel> {

  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    log.info(Constants.LOG_MSG + ch);
    ch.pipeline().addLast(new Socks01InitHandler());
  }
}