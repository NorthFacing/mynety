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
package com.shadowsocks.client;

import com.shadowsocks.client.config.ServerConfig;
import com.shadowsocks.common.encryption.ICrypt;
import com.shadowsocks.common.utils.SocksServerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;

/**
 * localServer接受到数据发送数据给remoteServer
 *
 * @author 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Slf4j
public final class OutRelayHandler extends ChannelInboundHandlerAdapter {

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    Channel remoteChannel = ctx.channel().attr(ServerConfig.REMOTE_CHANNEL).get();
    Boolean isProxy = ctx.channel().attr(ServerConfig.IS_PROXY).get();
    ICrypt crypt = ctx.channel().attr(ServerConfig.CRYPT_KEY).get();

    ByteBuf byteBuf = (ByteBuf) msg;
    try (ByteArrayOutputStream _remoteOutStream = new ByteArrayOutputStream()) {
      if (remoteChannel.isActive()) {

        if (!byteBuf.hasArray()) {
          int len = byteBuf.readableBytes();
          byte[] arr = new byte[len];
          byteBuf.getBytes(0, arr);

          if (isProxy) {
            crypt.encrypt(arr, arr.length, _remoteOutStream);
            arr = _remoteOutStream.toByteArray();
          }
          remoteChannel.writeAndFlush(Unpooled.wrappedBuffer(arr));
        }
      }
    } catch (Exception e) {
      log.error(LOG_MSG + ctx.channel() + " Send data to remoteServer error: ", e);
    } finally {
      ReferenceCountUtil.release(msg);
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    Channel remoteChannel = ctx.channel().attr(ServerConfig.REMOTE_CHANNEL).get();
    if (remoteChannel.isActive()) {
      SocksServerUtils.closeOnFlush(remoteChannel);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    log.error(LOG_MSG + ctx.channel(), cause);
    ctx.close();
  }

}
