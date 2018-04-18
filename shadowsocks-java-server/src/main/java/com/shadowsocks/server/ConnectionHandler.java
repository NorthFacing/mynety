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
package com.shadowsocks.server;

import com.shadowsocks.common.constants.Constants;
import com.shadowsocks.common.encryption.CryptUtil;
import com.shadowsocks.common.encryption.ICrypt;
import com.shadowsocks.server.Config.Config;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 连接处理器
 *
 * @author 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Slf4j
public class ConnectionHandler extends SimpleChannelInboundHandler {

  private AtomicReference<Channel> remoteChannel = new AtomicReference<>();
  private ByteBuf clientCache;

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {

    String host = ctx.channel().attr(Config.HOST).get();
    Integer port = ctx.channel().attr(Config.PORT).get();
    ICrypt crypt = ctx.channel().attr(Config.CRYPT_KEY).get();
    clientCache = ctx.channel().attr(Config.BUF).get();

    Bootstrap bootstrap = new Bootstrap();
    bootstrap.group(ctx.channel().eventLoop())
        .channel(Constants.channelClass)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast(new RemoteHandler(ctx, crypt, clientCache));
          }
        });

    try {
      final InetAddress inetAddress = InetAddress.getByName(host);
      ChannelFuture channelFuture = bootstrap.connect(inetAddress, port);
      channelFuture.addListener((ChannelFutureListener) future -> {
        if (future.isSuccess()) {
          log.debug("connect success host = " + host + ", port = " + port + ", inetAddress = " + inetAddress);
          remoteChannel.set(future.channel());
        } else {
          log.debug("connect fail host = " + host + ", port = " + port + ", inetAddress = " + inetAddress);
          future.cancel(true);
          channelClose();
        }
      });

    } catch (Exception e) {
      log.error("connect intenet error", e);
      channelClose();
    }

  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    ICrypt crypt = ctx.channel().attr(Config.CRYPT_KEY).get();

    ByteBuf buff = (ByteBuf) msg;
    if (buff.readableBytes() <= 0) {
      return;
    }
    byte[] decrypt = CryptUtil.decrypt(crypt, msg);
    if (remoteChannel.get() != null) {
      remoteChannel.get().writeAndFlush(Unpooled.wrappedBuffer(decrypt));
    } else {
      clientCache.writeBytes(decrypt);
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    ctx.close();
    log.info("ConnectionHandler channelInactive close");
    channelClose();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ctx.close();
    channelClose();
    log.error("ConnectionHandler error", cause);
  }

  private void channelClose() {
    try {
      if (remoteChannel.get() != null) {
        remoteChannel.get().close();
        remoteChannel = null;
      }
      if (clientCache != null) {
        clientCache.clear();
        clientCache = null;
      }
    } catch (Exception e) {
      log.error("close channel error", e);
    }
  }
}
