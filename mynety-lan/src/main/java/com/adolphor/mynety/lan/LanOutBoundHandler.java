package com.adolphor.mynety.lan;

import com.adolphor.mynety.common.bean.lan.LanMessage;
import com.adolphor.mynety.common.encryption.CryptUtil;
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

import static com.adolphor.mynety.common.constants.Constants.LOG_MSG;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG_IN;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG_OUT;
import static com.adolphor.mynety.common.constants.LanConstants.LAN_MSG_DISCONNECT;
import static com.adolphor.mynety.common.constants.LanConstants.LAN_MSG_TRANSFER;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * lan客户端与目的地址的连接，
 *
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
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.debug("[ {} ]【{}】调用 active 方法开始……", ctx.channel().id(), getSimpleName(this));
    super.channelActive(ctx);
    AtomicReference<ByteBuf> tempMsgRef = ChannelContainer.getTempMsgRef(requestId);
    if (tempMsgRef != null && tempMsgRef.get().readableBytes() > 0) {
      ByteBuf tempMsg = tempMsgRef.get();
      ctx.channel().writeAndFlush(tempMsg).addListener(future ->
          // 放在回调方法里是为了避免channel空指针异常
          logger.debug("[ {}{}{} ]【{}】获取并消费缓存消息……", inRelayChannel.id(), LOG_MSG_OUT, ctx.channel().id(), getSimpleName(this))
      );
    } else {
      logger.info("[ {}{}{} ] 无缓存消息", inRelayChannel.id(), LOG_MSG_OUT, ctx.channel().id());
    }
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    logger.debug("[ {}{}{} ]【{}】LAN客户端收到目的地址返回信息: {} bytes => {}", inRelayChannel.id(), LOG_MSG_IN, ctx.channel().id(), getSimpleName(this), msg.readableBytes(), ByteStrUtils.getByteArr((msg).copy()));
    try {
      byte[] encrypt = CryptUtil.encrypt(crypt, msg);
      LanMessage lanConnMsg = new LanMessage();
      lanConnMsg.setType(LAN_MSG_TRANSFER);
      lanConnMsg.setRequestId(requestId);
      lanConnMsg.setData(encrypt);
      inRelayChannel.writeAndFlush(lanConnMsg);
      logger.debug("[ {}{}{} ]【{}】将目的地址返回信息发送给ss服务端: {} bytes => {}", inRelayChannel.id(), LOG_MSG_IN, ctx.channel().id(), getSimpleName(this), msg.readableBytes(), ByteStrUtils.getByteArr((msg).copy()));
    } catch (Exception e) {
      logger.error("[ " + inRelayChannel + LOG_MSG_IN + ctx.channel() + " ] error", e);
      channelClose(ctx);
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    logger.debug("[ {}{}{} ]【{}】 内容读取完毕……", inRelayChannel.id(), LOG_MSG, ctx.channel().id(), getSimpleName(this));
  }

  /**
   * 常规情况下只需要断开 inRelayChannel 和 outRelayChannel，但本方法中不能直接断开两个链接，
   * 而是断开一个连接，另一个共用的channel不断开而是将其作为通道，发送断开指令
   *
   * @param ctx
   */
  @Override
  public void channelClose(ChannelHandlerContext ctx) {
    // 关闭之前先告诉lan服务端，断开用户和lan服务端的请求
    LanMessage disConnLanMsg = LanMsgUtils.packageLanMsg(inRelayChannel, requestId, LAN_MSG_DISCONNECT);
    inRelayChannel.writeAndFlush(disConnLanMsg);
    ChannelContainer.removeReqChannel(requestId);
    logger.info("[ {} ]【{}】发送断开连接信息给lan服务端，需要断开的requestId： {}", ctx.channel().id(), getSimpleName(this), requestId);
    // 断开本channel连接，要放在后面，防止断开之后不能发送信息给lan服务器
    super.channelClose(ctx);
  }

}
