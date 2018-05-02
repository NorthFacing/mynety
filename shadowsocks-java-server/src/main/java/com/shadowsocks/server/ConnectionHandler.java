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

import java.util.concurrent.atomic.AtomicReference;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;

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
  public void channelActive(ChannelHandlerContext clientCtx) throws Exception {

    Channel clientChannel = clientCtx.channel();

    String dstAddr = clientChannel.attr(Constants.ATTR_HOST).get();
    Integer dstPort = clientChannel.attr(Constants.ATTR_PORT).get();
    ICrypt crypt = clientChannel.attr(Constants.ATTR_CRYPT_KEY).get();
    clientCache = clientChannel.attr(Constants.ATTR_BUF).get();

    Bootstrap remoteBootStrap = new Bootstrap();
    remoteBootStrap.group(clientChannel.eventLoop())
        .channel(Constants.channelClass)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast(new RemoteHandler(clientCtx, crypt, clientCache));
          }
        });

    try {
      ChannelFuture channelFuture = remoteBootStrap.connect(dstAddr, dstPort);
      channelFuture.addListener((ChannelFutureListener) future -> {
        if (future.isSuccess()) {
          remoteChannel.set(future.channel());
          logger.debug("{} {} connect success dstAddr = {}, dstPort = {}", LOG_MSG, clientChannel, dstAddr, dstPort);
        } else {
          logger.debug("{} {} connect fail dstAddr = {}, dstPort = {}", LOG_MSG, clientChannel, dstAddr, dstPort);
          future.cancel(true);
          channelClose();
        }
      });

    } catch (Exception e) {
      logger.error(LOG_MSG + "connect intenet error", e);
      channelClose();
    }

  }

  @Override
  protected void channelRead0(ChannelHandlerContext clientCtx, Object msg) throws Exception {
    ICrypt crypt = clientCtx.channel().attr(Constants.ATTR_CRYPT_KEY).get();

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
    logger.info("ConnectionHandler channelInactive close");
    channelClose();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ctx.close();
    channelClose();
    logger.error(LOG_MSG + "ConnectionHandler error", cause);
  }

  @SuppressWarnings("Duplicates")
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
      logger.error(LOG_MSG + "close channel error", e);
    }
  }
}
