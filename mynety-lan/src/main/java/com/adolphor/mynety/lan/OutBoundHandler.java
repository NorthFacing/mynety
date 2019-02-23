package com.adolphor.mynety.lan;

import com.adolphor.mynety.common.bean.lan.LanMessage;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.utils.ByteStrUtils;
import com.adolphor.mynety.common.utils.LanMsgUtils;
import com.adolphor.mynety.common.wrapper.AbstractSimpleHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.ATTR_CRYPT_KEY;
import static com.adolphor.mynety.common.constants.Constants.ATTR_IN_RELAY_CHANNEL;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
public class OutBoundHandler extends AbstractSimpleHandler<ByteBuf> {

  public static final OutBoundHandler INSTANCE = new OutBoundHandler();

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    Channel inRelayChannel = ctx.channel().attr(ATTR_IN_RELAY_CHANNEL).get();
    ICrypt crypt = inRelayChannel.attr(ATTR_CRYPT_KEY).get();
    ByteBuf encryptBuf = crypt.encrypt(msg);
    byte[] data = ByteStrUtils.readArrayByBuf(encryptBuf);
    LanMessage lanMessage = LanMsgUtils.packTransmitMsg(data);
    inRelayChannel.writeAndFlush(lanMessage);
  }

}
