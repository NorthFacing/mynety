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
package com.shadowsocks.client;

import com.shadowsocks.common.encryption.ICrypt;
import com.shadowsocks.common.nettyWrapper.AbstractOutRelayHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;
import static com.shadowsocks.common.constants.Constants.LOG_MSG_IN;
import static com.shadowsocks.common.constants.Constants.LOG_MSG_OUT;
import static com.shadowsocks.common.constants.Constants.REQUEST_TEMP_LIST;

/**
 * 远程处理器，连接真正的目标地址
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Slf4j
public final class RemoteHandler extends AbstractOutRelayHandler<ByteBuf> {

  private final boolean isProxy;
  private final ICrypt _crypt;
  private final List requestTempList;

  public RemoteHandler(Channel clientProxyChannel, boolean isProxy, ICrypt _crypt) {
    super(clientProxyChannel);
    this.isProxy = isProxy;
    this._crypt = _crypt;
    this.requestTempList = clientChannel.attr(REQUEST_TEMP_LIST).get();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.info("[ {}{}{} ] [RemoteHandler-channelActive] channel active...", clientChannel, LOG_MSG, ctx.channel());
    if (requestTempList != null) {
      requestTempList.forEach(msg -> {
        ctx.channel().writeAndFlush(msg);
        logger.debug("[ {}{}{} ] [RemoteHandler-channelActive] write temp msg to des host: {}", clientChannel, LOG_MSG_OUT, ctx.channel(), msg);
      });
      requestTempList.clear();
    } else {
      logger.info("[ {}{}{} ] [RemoteHandler-channelActive] temp msg list is null...", clientChannel, LOG_MSG_OUT, ctx.channel());
    }
  }

  @Override
  @SuppressWarnings("Duplicates")
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    logger.debug("[ {}{}{} ] socks client remote channelRead: {} bytes => {}", clientChannel, LOG_MSG_IN, ctx.channel(), msg.readableBytes(), msg);
    if (!clientChannel.isOpen()) {
      channelClose(ctx);
      return;
    }
    try (ByteArrayOutputStream _localOutStream = new ByteArrayOutputStream()) {
      if (!msg.hasArray()) {
        int len = msg.readableBytes();
        byte[] temp = new byte[len];
        msg.getBytes(0, temp);
        if (isProxy) {
          logger.debug("[ {}{}{} ] msg need to decrypt...", clientChannel, LOG_MSG, ctx.channel());
          _crypt.decrypt(temp, temp.length, _localOutStream);
          temp = _localOutStream.toByteArray();
        }
        ByteBuf decryptedBuf = Unpooled.wrappedBuffer(temp);
        clientChannel.writeAndFlush(decryptedBuf);
        logger.debug("[ {}{}{} ] write to user-agent channel: {} bytes => {}", clientChannel, LOG_MSG_IN, ctx.channel(), decryptedBuf.readableBytes(), decryptedBuf);
      }
    } catch (Exception e) {
      logger.error("[ " + clientChannel + LOG_MSG_IN + ctx.channel() + " ] error", e);
      channelClose(ctx);
    }
  }

}
