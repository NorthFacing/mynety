package com.adolphor.mynety.server.lan;

import com.adolphor.mynety.common.bean.lan.LanMessageDecoder;
import com.adolphor.mynety.common.bean.lan.LanMessageEncoder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
@ChannelHandler.Sharable
public class LanOutBoundInitializer extends ChannelInitializer<SocketChannel> {

  public static final LanOutBoundInitializer INSTANCE = new LanOutBoundInitializer();

  @Override
  public void initChannel(SocketChannel ch) {
    ch.pipeline().addLast(new LanMessageDecoder());
    ch.pipeline().addLast(new LanMessageEncoder());
    ch.pipeline().addLast(new HeartBeatHandler());
    ch.pipeline().addLast(LanOutBoundHandler.INSTANCE);
  }
}