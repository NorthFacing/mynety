package com.adolphor.mynety.lan;

import com.adolphor.mynety.common.bean.lan.LanMessage;
import com.adolphor.mynety.common.utils.LanMsgUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.LanConstants.ALL_IDLE_TIME;
import static com.adolphor.mynety.common.constants.LanConstants.ATTR_LAST_BEAT_NO;
import static com.adolphor.mynety.common.constants.LanConstants.IS_MAIN_CHANNEL;
import static com.adolphor.mynety.common.constants.LanConstants.READ_IDLE_TIME;
import static com.adolphor.mynety.common.constants.LanConstants.WRITE_IDLE_TIME;

/**
 * heart beat: clint write timeout, server read timeout
 * long time no msg to reply, then send heart beat.
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

  /**
   * only main channel needs to keep long connection
   *
   * @param ctx
   * @param evt
   */
  @Override
  protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) {
    if (IdleStateEvent.WRITER_IDLE_STATE_EVENT.equals(evt)) {
      Boolean isMainChannel = ctx.channel().attr(IS_MAIN_CHANNEL).get();
      if (isMainChannel != null && isMainChannel) {
        Long sequenceNumber = LanMsgUtils.getNextNumber(ctx.channel());
        ctx.channel().attr(ATTR_LAST_BEAT_NO).set(sequenceNumber);
        LanMessage lanMessage = LanMsgUtils.packHeartBeatMsg(sequenceNumber);
        ctx.writeAndFlush(lanMessage);
      }
    }
  }

}
