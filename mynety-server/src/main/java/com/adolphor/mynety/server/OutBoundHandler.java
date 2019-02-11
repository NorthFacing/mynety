package com.adolphor.mynety.server;

import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.wrapper.AbstractOutBoundHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.ATTR_CRYPT_KEY;
import static com.adolphor.mynety.common.constants.Constants.ATTR_IN_RELAY_CHANNEL;

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
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
    Channel inRelayChannel = ctx.channel().attr(ATTR_IN_RELAY_CHANNEL).get();

    if (inRelayChannel != null && !inRelayChannel.isOpen()) {
      channelClose(ctx);
      return;
    }

    try {
      ICrypt crypt = inRelayChannel.attr(ATTR_CRYPT_KEY).get();
      ByteBuf encryptBuf = crypt.encrypt(msg);
      inRelayChannel.writeAndFlush(encryptBuf);
    } catch (Exception e) {
      logger.error("[ " + inRelayChannel.id() + Constants.LOG_MSG_IN + ctx.channel().id() + " ] error：", e);
      channelClose(ctx);
    }

  }

}
