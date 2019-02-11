package com.adolphor.mynety.lan;

import com.adolphor.mynety.common.bean.lan.LanMessage;
import com.adolphor.mynety.common.constants.LanMsgType;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.utils.ByteStrUtils;
import com.adolphor.mynety.common.utils.LanMsgUtils;
import com.adolphor.mynety.common.wrapper.AbstractSimpleHandler;
import com.adolphor.mynety.lan.utils.ChannelContainer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

import static com.adolphor.mynety.common.constants.Constants.LOG_MSG_IN;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
public class LanOutBoundHandler extends AbstractSimpleHandler<ByteBuf> {

  private final String requestId;
  private final Channel inRelayChannel;
  private final ICrypt crypt;

  public LanOutBoundHandler(Channel inRelayChannel, String requestId, ICrypt crypt) {
    this.crypt = crypt;
    this.requestId = requestId;
    this.inRelayChannel = inRelayChannel;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    try {
      super.channelActive(ctx);
      AtomicReference<ByteBuf> tempMsgRef = ChannelContainer.getTempMsgRef(requestId);
      if (tempMsgRef != null && tempMsgRef.get().readableBytes() > 0) {
        ByteBuf tempMsg = tempMsgRef.get();
        ctx.channel().writeAndFlush(tempMsg);
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      channelClose(ctx);
    }
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    try {
      ByteBuf encryptBuf = crypt.encrypt(msg);
      LanMessage lanConnMsg = new LanMessage();
      lanConnMsg.setType(LanMsgType.TRANSFER);
      lanConnMsg.setRequestId(requestId);
      lanConnMsg.setData(ByteStrUtils.getArrayByBuf(encryptBuf));
      inRelayChannel.writeAndFlush(lanConnMsg);
    } catch (Exception e) {
      logger.error("[ " + inRelayChannel + LOG_MSG_IN + ctx.channel() + " ] error", e);
      channelClose(ctx);
    }
  }

  /**
   * in general, <code>channelClose</code> method only need to closes inRelayChannel & outRelayChannel directly,
   * but in this method, inRelayChannel is the public channel, cannot be closed, only need to sends the
   * disconnection msg to socks server.
   *
   * @param ctx
   */
  @Override
  public void channelClose(ChannelHandlerContext ctx) {
    LanMessage disConnLanMsg = LanMsgUtils.packageLanMsg(inRelayChannel, requestId, LanMsgType.DISCONNECT);
    inRelayChannel.writeAndFlush(disConnLanMsg);
    // should waiting for the disconnection reply message ?
    ChannelContainer.removeReqChannel(requestId);
    super.channelClose(ctx);
  }

}
