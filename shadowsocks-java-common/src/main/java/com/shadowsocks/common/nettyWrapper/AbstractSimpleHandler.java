package com.shadowsocks.common.nettyWrapper;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * 主要是覆写增加了LOG日志和channel关闭抽象方法
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public abstract class AbstractSimpleHandler<I> extends SimpleChannelInboundHandler<I> {

  protected final long activeTime = System.currentTimeMillis();

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.info("[ {}{} ] [{}-channelActive] channel active...", ctx.channel(), LOG_MSG, getSimpleName(this));
    super.channelActive(ctx);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    channelClose(ctx);
    long connTime = System.currentTimeMillis() - activeTime;
    logger.info("[ {}{} ] [{}-channelInactive] channel inactive, channel closed, conn time: {}ms", ctx.channel(), LOG_MSG, getSimpleName(this), connTime);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    logger.debug("[ {}{} ] [{}-channelReadComplete] channelReadComplete...", ctx.channel(), LOG_MSG, getSimpleName(this));
    super.channelReadComplete(ctx);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    channelClose(ctx);
    logger.error("[ " + ctx.channel() + LOG_MSG + " ] " + getSimpleName(this) + " error", cause);
  }

  protected abstract void channelClose(ChannelHandlerContext ctx);

}
