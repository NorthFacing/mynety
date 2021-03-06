package com.adolphor.mynety.server.lan;

import com.adolphor.mynety.common.utils.ChannelUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.LanConstants.ALL_IDLE_TIME;
import static com.adolphor.mynety.common.constants.LanConstants.ATTR_LOST_BEAT_CNT;
import static com.adolphor.mynety.common.constants.LanConstants.MAX_IDLE_TIMES_LIMIT;
import static com.adolphor.mynety.common.constants.LanConstants.READ_IDLE_TIME;
import static com.adolphor.mynety.common.constants.LanConstants.WRITE_IDLE_TIME;

/**
 * heart beat: clint write timeout, server read timeout.
 * long time no msg replied, then mark
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
@ChannelHandler.Sharable
public class HeartBeatHandler extends IdleStateHandler {

  public static final HeartBeatHandler INSTANCE = new HeartBeatHandler();

  public HeartBeatHandler() {
    super(READ_IDLE_TIME, WRITE_IDLE_TIME, ALL_IDLE_TIME);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
    ctx.channel().attr(ATTR_LOST_BEAT_CNT).set(0L);
  }

  @Override
  protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
    if (IdleStateEvent.READER_IDLE_STATE_EVENT.equals(evt)) {
      Long lostBeatCount = ctx.channel().attr(ATTR_LOST_BEAT_CNT).get();
      if (lostBeatCount >= MAX_IDLE_TIMES_LIMIT) {
        ChannelUtils.closeOnFlush(ctx.channel());
      } else {
        ctx.channel().attr(ATTR_LOST_BEAT_CNT).set(++lostBeatCount);
      }
    }
    super.channelIdle(ctx, evt);
  }

}
