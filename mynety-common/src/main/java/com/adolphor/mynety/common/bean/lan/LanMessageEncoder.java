package com.adolphor.mynety.common.bean.lan;

import com.adolphor.mynety.common.constants.LanMsgType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

import static com.adolphor.mynety.common.constants.LanConstants.LENGTH_FIELD_LENGTH;

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

    ByteBuf tempData = Unpooled.buffer();
    LanMsgType type = lanMessage.getType();

    if (LanMsgType.CLIENT == type) {
      String password = lanMessage.getPassword();
      byte[] passwordArray = password.getBytes(StandardCharsets.UTF_8);
      tempData.writeBytes(passwordArray);
    } else if (LanMsgType.CONNECT == type) {
      tempData.writeBytes(lanMessage.getShortReqId());
      byte[] uri = lanMessage.getUri().getBytes(StandardCharsets.UTF_8);
      tempData.writeBytes(uri);
    } else if (LanMsgType.CONNECTED == type) {
      tempData.writeBytes(lanMessage.getShortReqId());
    } else if (LanMsgType.TRANSMIT == type) {
      tempData.writeBytes(lanMessage.getData());
    } else if (LanMsgType.HEARTBEAT == type) {
      tempData.writeBytes(lanMessage.getBytesSequenceNum());
    } else {
      throw new IllegalArgumentException("unsupported msg type: " + type);
    }
    // 4 bytes len header + 1 byte type + dynamic len data
    int totalLength = LENGTH_FIELD_LENGTH + 1 + tempData.readableBytes();
    out.writeInt(totalLength);
    out.writeByte(type.getVal());
    out.writeBytes(tempData);
  }

}