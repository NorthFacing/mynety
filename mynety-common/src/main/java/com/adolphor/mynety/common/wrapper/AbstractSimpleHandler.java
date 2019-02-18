package com.adolphor.mynety.common.wrapper;

import com.adolphor.mynety.common.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

import static com.adolphor.mynety.common.constants.Constants.ATTR_CONNECTED_TIMESTAMP;
import static com.adolphor.mynety.common.constants.Constants.ATTR_IN_RELAY_CHANNEL;
import static com.adolphor.mynety.common.constants.Constants.ATTR_OUT_RELAY_CHANNEL_REF;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_ADDRESS;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG_IN;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG_OUT;
import static org.apache.commons.lang3.ClassUtils.getName;

/**
 * override and implement some methods:
 * <p>
 * 1. add log infos
 * 2. add timestamp info
 * 3. add channel close method
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.4
 */
@Slf4j
@ChannelHandler.Sharable
public abstract class AbstractSimpleHandler<I> extends SimpleChannelInboundHandler<I> {

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    long timestamp = System.currentTimeMillis();
    ctx.channel().attr(ATTR_CONNECTED_TIMESTAMP).set(timestamp);
    logger.debug("[ {} ]{} set connect timestamp attr: {}", ctx.channel().id(), getName(this), timestamp + " => " + ctx.channel().attr(ATTR_REQUEST_ADDRESS).get());
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    AtomicReference<Channel> outRelayChannelRef = ctx.channel().attr(ATTR_OUT_RELAY_CHANNEL_REF).get();
    Channel inRelayChannel = ctx.channel().attr(ATTR_IN_RELAY_CHANNEL).get();
    if (outRelayChannelRef != null && outRelayChannelRef.get() != null) {
      logger.debug("[ {}{}{} ] {} read complete......", ctx.channel().id(), LOG_MSG_OUT, outRelayChannelRef.get().id(), getName(this));
    } else if (inRelayChannel != null) {
      logger.debug("[ {}{}{} ] {} read complete......", inRelayChannel.id(), LOG_MSG_IN, ctx.channel().id(), getName(this));
    } else {
      logger.debug("[ {} ] {} read complete......", ctx.channel().id(), getName(this));
    }
    super.channelReadComplete(ctx);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    logger.debug("[ {} ] {} call inactive method......", ctx.channel().id(), getName(this));
    super.channelInactive(ctx);
    channelClose(ctx);
  }

  /**
   * if subclass get an exception, should handles the exception by itself; if throws the exception out,
   * then the channel will be closed
   *
   * @param ctx
   * @param cause
   */
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    logger.warn("[ " + ctx.channel() + " ] [" + getName(this) + "] error", cause);
    channelClose(ctx);
  }

  protected void channelClose(ChannelHandlerContext ctx) {
    long connTime = System.currentTimeMillis() - ctx.channel().attr(ATTR_CONNECTED_TIMESTAMP).get();
    logger.info("[ {} ] {} channel will be closed, connection time: {} ms", ctx.channel(), getName(this), connTime);
    ChannelUtils.closeOnFlush(ctx.channel());
  }

}
