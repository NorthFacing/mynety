/**
 * MIT License
 * <p>
 * Copyright (c) 2018 0haizhu0@gmail.com
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
  public static final int PORT = Integer.parseInt(System.getProperty("port", "1086"));

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