package com.adolphor.mynety.common.bean.lan;

import com.adolphor.mynety.common.constants.LanMsgType;
import com.adolphor.mynety.common.utils.BaseUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * encode lan msg to byte
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
public class LanMessageEncoder extends MessageToByteEncoder<LanMessage> {

  @Override
  protected void encode(ChannelHandlerContext ctx, LanMessage lanMessage, ByteBuf out) throws Exception {

    int frameLength = LanMessage.HEADER_SIZE;
    if (lanMessage.getUri() != null) {
      frameLength += lanMessage.getUri().getBytes(StandardCharsets.UTF_8).length;
    }
    if (lanMessage.getData() != null) {
      frameLength += lanMessage.getData().length;
    }

    out.writeInt(frameLength);
    out.writeByte(lanMessage.getType().getVal());
    out.writeLong(lanMessage.getSequenceNumber());
    if (StringUtils.isNotEmpty(lanMessage.getRequestId())) {
      byte[] comReqId = BaseUtils.compressUUID(lanMessage.getRequestId());
      out.writeBytes(comReqId);
    } else {
      out.writeInt(0);
    }
    if (lanMessage.getType() == LanMsgType.CONNECT) {
      out.writeBytes(lanMessage.getUri().getBytes(StandardCharsets.UTF_8));
    } else {
      if (lanMessage.getData() != null) {
        out.writeBytes(lanMessage.getData());
      }
    }
  }

}