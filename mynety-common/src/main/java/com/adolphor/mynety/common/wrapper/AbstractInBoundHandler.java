package com.adolphor.mynety.common.wrapper;

import com.adolphor.mynety.common.utils.ByteStrUtils;
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
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * 增加日志以及关闭连接的时候释放缓存
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.4
 */
@Slf4j
public abstract class AbstractInBoundHandler<I> extends AbstractSimpleHandler<I> {

  /**
   * 本方法没增加任何逻辑，只为了说明架构公共代码逻辑，可以删除没有任何影响。
   * <p>
   * 本方法的作用是，建立远程连接，且在 future.isSuccess() 中建立 inRelay 和 outRelay 的相互引用关联，关键代码：
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
    logger.debug("[ {} ]【{}】调用 active 方法开始……", ctx.channel().id(), getSimpleName(this));
    super.channelActive(ctx);
  }

  /**
   * 本方法没增加任何逻辑，只为了说明架构公共代码逻辑，可以删除没有任何影响。
   * <p>
   * 本方法的作用，将接收到的消息发送给远程目的地址或存储到缓存列表。
   * 远程目的地址和缓存列表都需要从inRelayChannel中获取。
   * <p>
   * AtomicReference<Channel> outRelayChannelRef = ctx.channel().attr(ATTR_OUT_RELAY_CHANNEL_REF).get();
   * List requestTempList = ctx.channel().attr(ATTR_REQUEST_TEMP_LIST).get();
   * outRelayChannelRef.get().writeAndFlush(msg);
   * 或
   * requestTempList.add(decryptBuf);
   *
   * @param ctx
   * @param msg
   * @throws Exception
   */
  @Override
  protected abstract void channelRead0(ChannelHandlerContext ctx, I msg) throws Exception;

  /**
   * 关闭channel
   *
   * @param ctx
   */
  @Override
  protected void channelClose(ChannelHandlerContext ctx) {
    AtomicReference<Channel> outRelayChannelRef = ctx.channel().attr(ATTR_OUT_RELAY_CHANNEL_REF).get();

    // 释放缓存
    AtomicReference tempMsgRef = ctx.channel().attr(ATTR_REQUEST_TEMP_MSG).get();
    if (tempMsgRef != null) {
      if (tempMsgRef.get() instanceof ByteBuf) {
        Object tempMsg = tempMsgRef.get();
        if (((ByteBuf) tempMsg).refCnt() > 0) {
          logger.info("[ {}{}{} ]【{}】inRelayChannel 将要关闭连接，释放缓存: {} bytes => {}", ctx.channel().id(), LOG_MSG, ((outRelayChannelRef != null && outRelayChannelRef.get() != null) ? outRelayChannelRef.get().id() : ""), getSimpleName(this), ((ByteBuf) tempMsg).readableBytes(), ByteStrUtils.getArrByDirectBuf(((ByteBuf) tempMsg).copy()));
          ReferenceCountUtil.release(tempMsg);
        } else {
          logger.info("[ {}{}{} ]【{}】inRelayChannel 将要关闭连接，没有需要释放的缓存……", ctx.channel().id(), LOG_MSG, ((outRelayChannelRef != null && outRelayChannelRef.get() != null) ? outRelayChannelRef.get().id() : ""), getSimpleName(this));
        }
      } else {
        logger.info("[ {}{}{} ]【{}】inRelayChannel 将要关闭连接，缓存将要丢弃: {} bytes => {}", ctx.channel().id(), LOG_MSG, ((outRelayChannelRef != null && outRelayChannelRef.get() != null) ? outRelayChannelRef.get().id() : ""), getSimpleName(this), getSimpleName(tempMsgRef.get()));
      }
    }

    if (outRelayChannelRef != null && outRelayChannelRef.get() != null && outRelayChannelRef.get().isActive()) {
      logger.info("[ {}{}{} ]【{}】调用 outRelayChannel 关闭方法，准备关闭……", ctx.channel().id(), LOG_MSG, outRelayChannelRef.get().id(), getSimpleName(this));
      ChannelUtils.closeOnFlush(outRelayChannelRef.get());
    }

    long connTime = System.currentTimeMillis() - ctx.channel().attr(ATTR_CONNECTED_TIMESTAMP).get();
    logger.info("[ {}{}{} ]【{}】inRelayChannel 关闭连接，共计连接时间: {}ms", ctx.channel().id(), LOG_MSG, ((outRelayChannelRef != null && outRelayChannelRef.get() != null) ? outRelayChannelRef.get().id() : ""), getSimpleName(this), connTime);
    ChannelUtils.closeOnFlush(ctx.channel());

  }

}
