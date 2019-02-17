package com.adolphor.mynety.common.wrapper;

import com.adolphor.mynety.common.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

import static com.adolphor.mynety.common.constants.Constants.ATTR_OUT_RELAY_CHANNEL_REF;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_TEMP_MSG;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG;
import static org.apache.commons.lang3.ClassUtils.getName;

/**
 * set default value when init
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
@ChannelHandler.Sharable
public abstract class AbstractInBoundInitializer extends ChannelInitializer<SocketChannel> {

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    final AtomicReference<Channel> outRelayChannelRef = new AtomicReference<>();
    ch.attr(ATTR_OUT_RELAY_CHANNEL_REF).set(outRelayChannelRef);
    AtomicReference tempMsgRef = new AtomicReference();
    ch.attr(ATTR_REQUEST_TEMP_MSG).set(tempMsgRef);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    logger.error("[ " + ctx.channel().id() + LOG_MSG + "] {} error: ", cause, getName(this));
    ChannelUtils.closeOnFlush(ctx.channel());
  }

}
