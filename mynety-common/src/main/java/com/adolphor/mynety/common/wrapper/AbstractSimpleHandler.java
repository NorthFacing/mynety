package com.adolphor.mynety.common.wrapper;

import com.adolphor.mynety.common.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

import static com.adolphor.mynety.common.constants.Constants.ATTR_CONNECTED_TIMESTAMP;
import static com.adolphor.mynety.common.constants.Constants.ATTR_OUT_RELAY_CHANNEL_REF;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * 主要是覆写增加了LOG日志和channel关闭抽象方法
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
    logger.debug("[ {} ]【{}】调用 active 方法开始……", ctx.channel(), getSimpleName(this));
    super.channelActive(ctx);
    long timestamp = System.currentTimeMillis();
    ctx.channel().attr(ATTR_CONNECTED_TIMESTAMP).set(timestamp);
    logger.debug("[ {} ]【{}】设置 connect timestamp 属性：{}", ctx.channel().id(), getSimpleName(this), timestamp);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    AtomicReference<Channel> outRelayChannelRef = ctx.channel().attr(ATTR_OUT_RELAY_CHANNEL_REF).get();
    logger.debug("[ {}{}{} ]【{}】 内容读取完毕……", ctx.channel().id(), LOG_MSG, (outRelayChannelRef != null && outRelayChannelRef.get() != null) ? outRelayChannelRef.get().id() : "", getSimpleName(this));
    super.channelReadComplete(ctx);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    logger.debug("[ {} ]【{}】调用 inactive 方法，将要关闭连接……", ctx.channel().id(), getSimpleName(this));
    channelClose(ctx);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    logger.debug("[ {} ]【{}】调用 exception 方法，将要关闭连接……", ctx.channel().id(), getSimpleName(this));
    channelClose(ctx);
    logger.error("[ " + ctx.channel().id() + " ]【" + getSimpleName(this) + "】error", cause);
  }

  protected void channelClose(ChannelHandlerContext ctx) {
    long connTime = System.currentTimeMillis() - ctx.channel().attr(ATTR_CONNECTED_TIMESTAMP).get();
    logger.info("[ {} ]【{}】关闭连接，共计连接时间: {}ms", ctx.channel().id(), getSimpleName(this), connTime);
    ChannelUtils.closeOnFlush(ctx.channel());
  }

}
