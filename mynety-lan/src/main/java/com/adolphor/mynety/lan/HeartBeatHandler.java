package com.adolphor.mynety.lan;

import com.adolphor.mynety.common.bean.lan.LanMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.LanConstants.ALL_IDLE_TIME;
import static com.adolphor.mynety.common.constants.LanConstants.ATTR_LAST_BEAT_NO;
import static com.adolphor.mynety.common.constants.LanConstants.LAN_MSG_HEARTBEAT;
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
    if (IdleStateEvent.WRITER_IDLE_STATE_EVENT.equals(evt)) {
      logger.info("[ {} ] [HeartBeatHandler-channelIdle] read timeout evt...", ctx.channel());
      Long incredSerNo = LanMessage.getIncredSerNo(ctx.channel());
      ctx.channel().attr(ATTR_LAST_BEAT_NO).set(incredSerNo);

      LanMessage beatMsg = new LanMessage();
      beatMsg.setType(LAN_MSG_HEARTBEAT);
      beatMsg.setSerialNumber(incredSerNo);
      logger.info("[ {} ] [HeartBeatHandler-channelIdle] write heart beat msg: {}", ctx.channel(), beatMsg);
      ctx.writeAndFlush(beatMsg);
    } else if (IdleStateEvent.READER_IDLE_STATE_EVENT.equals(evt)) {
      logger.info("[ {} ] [HeartBeatHandler-channelIdle] write timeout evt...", ctx.channel());
    }
    super.channelIdle(ctx, evt);
  }

}
