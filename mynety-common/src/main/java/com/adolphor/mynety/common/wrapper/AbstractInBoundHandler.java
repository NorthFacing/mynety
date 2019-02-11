package com.adolphor.mynety.common.wrapper;

import com.adolphor.mynety.common.utils.ChannelUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

import static com.adolphor.mynety.common.constants.Constants.ATTR_CONNECTED_TIMESTAMP;
import static com.adolphor.mynety.common.constants.Constants.ATTR_OUT_RELAY_CHANNEL_REF;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_TEMP_MSG;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG;
import static org.apache.commons.lang3.ClassUtils.getName;

/**
 * AbstractInBoundHandler, add logger and close method
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.4
 */
@Slf4j
public abstract class AbstractInBoundHandler<I> extends AbstractSimpleHandler<I> {

  /**
   * The functionality of the implement of this method:
   * build the connection to remote address, and after 'future.isSuccess()', link inRelay and outRelay to each other,
   * the main code is show as below:
   * <p>
   * Channel outRelayChannel = future.channel();
   * ctx.channel().attr(ATTR_OUT_RELAY_CHANNEL_REF).get().set(outRelayChannel);
   * outRelayChannel.attr(ATTR_IN_RELAY_CHANNEL).set(ctx.channel());
   *
   * @param ctx
   * @throws Exception
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.debug("[ {} ] {} call active method......", ctx.channel().id(), getName(this));
    super.channelActive(ctx);
  }

  /**
   * The functionality of the implement of this method:
   * <p>
   * after receive a msg:
   * 1. if the remote channel has not be built, put the msg the cache
   * 2. if built success, send to remote server
   * <p>
   * requestTempList.add(decryptBuf);
   * OR：
   * AtomicReference<Channel> outRelayChannelRef = ctx.channel().attr(ATTR_OUT_RELAY_CHANNEL_REF).get();
   * List requestTempList = ctx.channel().attr(ATTR_REQUEST_TEMP_LIST).get();
   * outRelayChannelRef.get().writeAndFlush(msg);
   *
   * @param ctx
   * @param msg
   * @throws Exception
   */
  @Override
  protected abstract void channelRead0(ChannelHandlerContext ctx, I msg) throws Exception;

  /**
   * disconnect, close channel
   *
   * @param ctx
   */
  @Override
  protected void channelClose(ChannelHandlerContext ctx) {
    AtomicReference<Channel> outRelayChannelRef = ctx.channel().attr(ATTR_OUT_RELAY_CHANNEL_REF).get();

    AtomicReference tempMsgRef = ctx.channel().attr(ATTR_REQUEST_TEMP_MSG).get();
    if (tempMsgRef != null && tempMsgRef.get() != null && tempMsgRef.get() instanceof ByteBuf) {
      ByteBuf tempMsg = (ByteBuf) tempMsgRef.get();
      if (tempMsg.refCnt() > 0) {
        ReferenceCountUtil.release(tempMsg);
      }
    }

    if (outRelayChannelRef != null && outRelayChannelRef.get() != null && outRelayChannelRef.get().isActive()) {
      long connTime = System.currentTimeMillis() - ctx.channel().attr(ATTR_CONNECTED_TIMESTAMP).get();
      logger.info("[ {}{}{} ] {} outRelayChannel will be closed, connection time: {}ms", LOG_MSG, outRelayChannelRef.get(), getName(this), connTime);
      ChannelUtils.closeOnFlush(outRelayChannelRef.get());
    }

    super.channelClose(ctx);
  }

}
