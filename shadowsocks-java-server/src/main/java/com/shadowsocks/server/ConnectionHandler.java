/**
 * MIT License
 * <p>
 * Copyright (c) Bob.Zhu
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

import com.shadowsocks.common.bean.Address;
import com.shadowsocks.common.constants.Constants;
import com.shadowsocks.common.encryption.CryptUtil;
import com.shadowsocks.common.encryption.ICrypt;
import com.shadowsocks.common.nettyWrapper.AbstractInRelayHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;
import static com.shadowsocks.common.constants.Constants.LOG_MSG_OUT;
import static com.shadowsocks.common.constants.Constants.REQUEST_ADDRESS;
import static com.shadowsocks.common.constants.Constants.REQUEST_TEMP_LIST;

/**
 * 连接处理器
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Slf4j
public class ConnectionHandler extends AbstractInRelayHandler<ByteBuf> {

  public ConnectionHandler(ByteBuf msg) {
    if (msg.readableBytes() > 0) {
      requestTempLists.add(msg.retain());
      logger.debug("[ {} ] [ConnectionHandler-constructor] add socks client request to temp list: {}", LOG_MSG, msg);
    } else {
      logger.debug("[ {} ] [ConnectionHandler-constructor] discard empty msg: {}", LOG_MSG, msg);
    }
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.info("[ {}{}{} ] [ConnectionHandler-channelActive] channel active...", ctx.channel(), LOG_MSG, remoteChannelRef.get());

    Channel clientChannel = ctx.channel();
    clientChannel.attr(REQUEST_TEMP_LIST).set(requestTempLists);

    ICrypt crypt = clientChannel.attr(Constants.ATTR_CRYPT_KEY).get();
    Address address = clientChannel.attr(REQUEST_ADDRESS).get();
    String dstAddr = address.getHost();
    Integer dstPort = address.getPort();

    Bootstrap remoteBootStrap = new Bootstrap();
    remoteBootStrap.group(clientChannel.eventLoop())
        .channel(Constants.channelClass)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast(new RemoteHandler(ctx.channel(), crypt));
            logger.info("[ {}{}{} ] [ConnectionHandler-channelActive] out pipeline add handler: RemoteHandler", ctx.channel(), LOG_MSG, ch);
          }
        });

    try {
      ChannelFuture channelFuture = remoteBootStrap.connect(dstAddr, dstPort);
      channelFuture.addListener((ChannelFutureListener) future -> {
        if (future.isSuccess()) {
          remoteChannelRef.set(future.channel());
          logger.debug("{}{} [ConnectionHandler-channelActive] connect to dst host success => {}:{}", LOG_MSG_OUT, clientChannel, dstAddr, dstPort);
        } else {
          logger.debug("{}{} [ConnectionHandler-channelActive] connect to dst host failed => {}:{}", LOG_MSG, clientChannel, dstAddr, dstPort);
          future.cancel(true);
          channelClose(ctx);
        }
      });

    } catch (Exception e) {
      logger.error(LOG_MSG + "connect internet error", e);
      channelClose(ctx);
    }

  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    Channel remoteChannel = remoteChannelRef.get();
    logger.debug("[ {}{}{} ] [ConnectionHandler-channelRead0] received msg: {} bytes => {}", ctx.channel(), LOG_MSG, remoteChannel, msg.readableBytes(), msg);
    ICrypt crypt = ctx.channel().attr(Constants.ATTR_CRYPT_KEY).get();
    byte[] temp = CryptUtil.decrypt(crypt, msg);
    ByteBuf decryptBuf = Unpooled.wrappedBuffer(temp);
    logger.debug("[ {}{}{} ] [ConnectionHandler-channelRead0] msg after decrypt: {} bytes => {}", ctx.channel(), LOG_MSG, remoteChannel, decryptBuf.readableBytes(), decryptBuf);
    synchronized (requestTempLists) {
      if (remoteChannel != null) {
        remoteChannel.writeAndFlush(decryptBuf);
        logger.debug("[ {}{}{} ] [ConnectionHandler-channelRead0] write msg to dst host channel: {} bytes => {}", ctx.channel(), LOG_MSG_OUT, remoteChannelRef.get(), decryptBuf.readableBytes(), decryptBuf);
      } else {
        requestTempLists.add(decryptBuf);
        logger.debug("[ {}{}{} ] [ConnectionHandler-channelRead0] add msg to temp list: {}", ctx.channel(), LOG_MSG, remoteChannel, msg);
      }
    }
  }

}
