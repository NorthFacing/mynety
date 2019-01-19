package com.adolphor.mynety.client;

import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.wrapper.AbstractOutBoundHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;

import static com.adolphor.mynety.common.constants.Constants.ATTR_CRYPT_KEY;
import static com.adolphor.mynety.common.constants.Constants.ATTR_IN_RELAY_CHANNEL;
import static com.adolphor.mynety.common.constants.Constants.ATTR_IS_PROXY;

/**
 * 远程处理器，连接真正的目标地址
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
@ChannelHandler.Sharable
public final class OutBoundHandler extends AbstractOutBoundHandler<ByteBuf> {

  public static final OutBoundHandler INSTANCE = new OutBoundHandler();

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    Channel inRelayChannel = ctx.channel().attr(ATTR_IN_RELAY_CHANNEL).get();
    logger.debug("[ {}{}{} ]【SocksOutBound 收到请求结果】内容: {} bytes => {}", (inRelayChannel != null ? inRelayChannel.id() : ""), Constants.LOG_MSG_IN, ctx.channel().id(), msg.readableBytes(), msg);

    Boolean isProxy = inRelayChannel.attr(ATTR_IS_PROXY).get();

    if (!inRelayChannel.isOpen()) {
      channelClose(ctx);
      return;
    }
    try (ByteArrayOutputStream _localOutStream = new ByteArrayOutputStream()) {
      if (!msg.hasArray()) {
        int len = msg.readableBytes();
        byte[] temp = new byte[len];
        msg.getBytes(0, temp);

        if (isProxy) {
          logger.debug("[ {}{}{} ]【SocksOutBound 收到请求结果】需要解密...", inRelayChannel.id(), Constants.LOG_MSG, ctx.channel().id());
          ICrypt crypt = inRelayChannel.attr(ATTR_CRYPT_KEY).get();
          crypt.decrypt(temp, temp.length, _localOutStream);
          temp = _localOutStream.toByteArray();
        }

        ByteBuf decryptedBuf = Unpooled.wrappedBuffer(temp);
        inRelayChannel.writeAndFlush(decryptedBuf);
        logger.debug("[ {}{}{} ]【SocksOutBound 收到请求结果】发送给客户端: {} bytes => {}", inRelayChannel.id(), Constants.LOG_MSG_IN, ctx.channel().id(), decryptedBuf.readableBytes(), decryptedBuf);
      }
    } catch (Exception e) {
      logger.error("[ " + inRelayChannel.id() + Constants.LOG_MSG_IN + ctx.channel().id() + " ] error", e);
      channelClose(ctx);
    }
  }

}
