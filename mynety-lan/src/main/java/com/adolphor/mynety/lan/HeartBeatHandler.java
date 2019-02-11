package com.adolphor.mynety.lan;

import com.adolphor.mynety.common.bean.lan.LanMessage;
import com.adolphor.mynety.common.constants.LanMsgType;
import com.adolphor.mynety.common.utils.LanMsgUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.LanConstants.ALL_IDLE_TIME;
import static com.adolphor.mynety.common.constants.LanConstants.ATTR_LAST_BEAT_NO;
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
  protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt){
    if (IdleStateEvent.WRITER_IDLE_STATE_EVENT.equals(evt)) {
      Long sequenceNumber = LanMsgUtils.getNextNumber(ctx.channel());
      ctx.channel().attr(ATTR_LAST_BEAT_NO).set(sequenceNumber);

      LanMessage beatMsg = new LanMessage();
      beatMsg.setType(LanMsgType.HEARTBEAT);
      beatMsg.setSequenceNumber(sequenceNumber);
      ctx.writeAndFlush(beatMsg);
    }
  }

}
