package com.adolphor.mynety.server.lan;

import com.adolphor.mynety.common.utils.ChannelUtils;
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
 * 心跳处理器（客户端写超时，服务端读超时）
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
public class HeartBeatHandler extends IdleStateHandler {

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
    Long lostBeatCnt = ctx.channel().attr(ATTR_LOST_BEAT_CNT).get();
    if (IdleStateEvent.READER_IDLE_STATE_EVENT.equals(evt)) {
      logger.info("[ {} ] read timeout evt...", ctx.channel().id());
      // 连续丢失最大容忍心跳包 (断开连接)
      if (lostBeatCnt >= MAX_IDLE_TIMES_LIMIT) {
        ChannelUtils.closeOnFlush(ctx.channel());
        logger.debug("[ {} ] over MAX_IDLE_TIMES_LIMIT, channel close", ctx.channel().id());
      } else {
        ctx.channel().attr(ATTR_LOST_BEAT_CNT).set(++lostBeatCnt);
        logger.debug("[ {} ] lost the {}th heart beat...", ctx.channel().id(), lostBeatCnt);
      }
    } else {
      logger.info("[ {} ] write timeout evt...", ctx.channel().id());
    }
    super.channelIdle(ctx, evt);
  }

}
