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

import com.shadowsocks.common.encryption.ICrypt;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;

/**
 * 远程处理器，连接真正的目标地址
 *
 * @author 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Slf4j
public final class RemoteHandler extends SimpleChannelInboundHandler<ByteBuf> {

  private final ChannelHandlerContext clientChannel;
  private final boolean isProxy;
  private final ICrypt _crypt;

  public RemoteHandler(ChannelHandlerContext clientProxyChannel, boolean isProxy, ICrypt _crypt) {
    this.clientChannel = clientProxyChannel;
    this.isProxy = isProxy;
    this._crypt = _crypt;
  }

  @Override
  @SuppressWarnings("Duplicates")
  protected void channelRead0(ChannelHandlerContext remoteCtx, ByteBuf msg) throws Exception {
    try (ByteArrayOutputStream _localOutStream = new ByteArrayOutputStream()) {
      if (!msg.hasArray()) {
        int len = msg.readableBytes();
        byte[] arr = new byte[len];
        msg.getBytes(0, arr);
        if (isProxy) {
          _crypt.decrypt(arr, arr.length, _localOutStream);
          arr = _localOutStream.toByteArray();
        }
        clientChannel.writeAndFlush(Unpooled.wrappedBuffer(arr));
      }
    } catch (Exception e) {
      logger.error(LOG_MSG + remoteCtx.channel() + " Receive remoteServer data error: ", e);
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    ctx.close();
    channelClose();
    logger.info("RemoteHandler channelInactive close");
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
    ctx.close();
    channelClose();
    logger.error("RemoteHandler error", throwable);
  }

  @SuppressWarnings("Duplicates")
  private void channelClose() {
    try {
      clientChannel.close();
    } catch (Exception e) {
      logger.error("close channel error", e);
    }
  }

}
