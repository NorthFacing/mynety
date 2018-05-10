package com.adolphor.mynety.lan;

import com.adolphor.mynety.common.bean.lan.LanMessage;
import com.adolphor.mynety.common.encryption.CryptUtil;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.wrapper.AbstractOutRelayHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.adolphor.mynety.common.constants.Constants.LOG_MSG;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG_IN;
import static com.adolphor.mynety.common.constants.LanConstants.LAN_MSG_TRANSFER;

/**
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.5
 */
@Slf4j
public class LanRemoteHandler extends AbstractOutRelayHandler<ByteBuf> {

  private final String requestId;

  public LanRemoteHandler(Channel clientChannel, ICrypt _crypt, String requestId, List requestTempList) {
    super(clientChannel, _crypt, requestTempList);
    this.requestId = requestId;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    logger.debug("[ {}{}{} ] [LanRemoteHandler-channelRead0] lan client remote channelRead: {} bytes => {}", clientChannel, LOG_MSG_IN, ctx.channel(), msg.readableBytes(), msg);
    try {
      logger.debug("[ {}{}{} ] [LanRemoteHandler-channelRead0] msg need to encrypt...", clientChannel, LOG_MSG, ctx.channel());
      byte[] encrypt = CryptUtil.encrypt(_crypt, msg);
      LanMessage lanConnMsg = new LanMessage();
      lanConnMsg.setType(LAN_MSG_TRANSFER);
      lanConnMsg.setRequestId(requestId);
      lanConnMsg.setData(encrypt);
      clientChannel.writeAndFlush(lanConnMsg);
      logger.debug("[ {}{}{} ] [LanRemoteHandler-channelRead0] write to socks server channel: {} bytes => {}", clientChannel, LOG_MSG_IN, ctx.channel(), msg.readableBytes(), msg);
    } catch (Exception e) {
      logger.error("[ " + clientChannel + LOG_MSG_IN + ctx.channel() + " ] error", e);
      channelClose(ctx);
    }
  }

}
