package com.adolphor.mynety.client;

import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.wrapper.AbstractOutRelayHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;

/**
 * 远程处理器，连接真正的目标地址
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
public final class RemoteHandler extends AbstractOutRelayHandler<ByteBuf> {

  private final boolean isProxy;

  public RemoteHandler(Channel clientProxyChannel, boolean isProxy, ICrypt _crypt) {
    super(clientProxyChannel,_crypt);
    this.isProxy = isProxy;
  }

  @Override
  @SuppressWarnings("Duplicates")
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    logger.debug("[ {}{}{} ] socks client remote channelRead: {} bytes => {}", clientChannel, Constants.LOG_MSG_IN, ctx.channel(), msg.readableBytes(), msg);
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
          logger.debug("[ {}{}{} ] msg need to decrypt...", clientChannel, Constants.LOG_MSG, ctx.channel());
          _crypt.decrypt(temp, temp.length, _localOutStream);
          temp = _localOutStream.toByteArray();
        }
        ByteBuf decryptedBuf = Unpooled.wrappedBuffer(temp);
        clientChannel.writeAndFlush(decryptedBuf);
        logger.debug("[ {}{}{} ] write to user-agent channel: {} bytes => {}", clientChannel, Constants.LOG_MSG_IN, ctx.channel(), decryptedBuf.readableBytes(), decryptedBuf);
      }
    } catch (Exception e) {
      logger.error("[ " + clientChannel + Constants.LOG_MSG_IN + ctx.channel() + " ] error", e);
      channelClose(ctx);
    }
  }

}
