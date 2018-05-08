package com.shadowsocks.client;

import com.shadowsocks.common.encryption.ICrypt;
import com.shadowsocks.common.nettyWrapper.AbstractOutRelayHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;

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

  private final boolean isProxy;
  private final ICrypt _crypt;

  public RemoteHandler(Channel clientProxyChannel, boolean isProxy, ICrypt _crypt) {
    super(clientProxyChannel);
    this.isProxy = isProxy;
    this._crypt = _crypt;
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
