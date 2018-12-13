package com.adolphor.mynety.server.lan;

import com.adolphor.mynety.common.utils.SocksServerUtils;
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
  protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {

    Long lostBeatCnt = ctx.channel().attr(ATTR_LOST_BEAT_CNT).get();
    lostBeatCnt = (lostBeatCnt == null) ? 0 : lostBeatCnt;

    if (IdleStateEvent.READER_IDLE_STATE_EVENT.equals(evt)) {
      logger.info("[ {} ] [HeartBeatHandler-channelIdle] read timeout evt...", ctx.channel());
      // 连续丢失最大容忍心跳包 (断开连接)
      if (lostBeatCnt >= MAX_IDLE_TIMES_LIMIT) {
        SocksServerUtils.closeOnFlush(ctx.channel());
        logger.debug("[ {} ] [HeartBeatHandler-channelIdle] over MAX_IDLE_TIMES_LIMIT, channel close", ctx.channel());
      } else {
        ctx.channel().attr(ATTR_LOST_BEAT_CNT).set(++lostBeatCnt);
        logger.debug("[ {} ] [HeartBeatHandler-channelIdle] lost the {}th heart beat...", ctx.channel(), lostBeatCnt);
      }
    } else {
      logger.info("[ {} ] [HeartBeatHandler-channelIdle] write timeout evt...", ctx.channel());
    }
    super.channelIdle(ctx, evt);
  }

}
