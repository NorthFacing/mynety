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

import com.shadowsocks.common.encryption.CryptUtil;
import com.shadowsocks.common.encryption.ICrypt;
import com.shadowsocks.common.nettyWrapper.AbstractOutRelayHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;
import static com.shadowsocks.common.constants.Constants.LOG_MSG_IN;

/**
 * 远程处理器，连接真正的目标地址
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Slf4j
public final class RemoteHandler extends AbstractOutRelayHandler<ByteBuf> {

  private final ICrypt _crypt;

  public RemoteHandler(Channel clientChannel, ICrypt _crypt) {
    super(clientChannel);
    this._crypt = _crypt;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    logger.debug("[ {}{}{} ] [RemoteHandler-channelRead0] socks server remote channelRead: {} bytes => {}", clientChannel, LOG_MSG_IN, ctx.channel(), msg.readableBytes(), msg);
    if (!clientChannel.isOpen()) {
      channelClose(ctx);
      return;
    }
    try {
      logger.debug("[ {}{}{} ] [RemoteHandler-channelRead0] msg need to encrypt...", clientChannel, LOG_MSG, ctx.channel());
      byte[] encrypt = CryptUtil.encrypt(_crypt, msg);
      clientChannel.writeAndFlush(Unpooled.wrappedBuffer(encrypt));
      logger.debug("[ {}{}{} ] [RemoteHandler-channelRead0] write to socks client channel: {} bytes => {}", clientChannel, LOG_MSG_IN, ctx.channel(), msg.readableBytes(), msg);
    } catch (Exception e) {
      logger.error("[ " + clientChannel + LOG_MSG_IN + ctx.channel() + " ] error", e);
      channelClose(ctx);
    }
  }

}
