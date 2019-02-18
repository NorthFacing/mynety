package com.adolphor.mynety.common.bean.lan;

import com.adolphor.mynety.common.constants.LanMsgType;
import com.adolphor.mynety.common.utils.ByteStrUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.LanConstants.INITIAL_BYTES_TO_STRIP;
import static com.adolphor.mynety.common.constants.LanConstants.LENGTH_ADJUSTMENT;
import static com.adolphor.mynety.common.constants.LanConstants.LENGTH_FIELD_LENGTH;
import static com.adolphor.mynety.common.constants.LanConstants.LENGTH_FIELD_OFFSET;
import static com.adolphor.mynety.common.constants.LanConstants.MAX_FRAME_LENGTH;

/**
 * decode byte to lan msg
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
public class LanMessageDecoder extends LengthFieldBasedFrameDecoder {

  private static final int requestIdLen = 16;

  public LanMessageDecoder() {
    super(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP);
  }

  @Override
  protected LanMessage decode(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

    ByteBuf byteBuf = (ByteBuf) super.decode(ctx, msg);
    if (byteBuf == null) {
      return null;
    }

    byte typeByte = byteBuf.readByte();
    LanMsgType type = LanMsgType.getType(typeByte);

    LanMessage lanMessage = new LanMessage();
    lanMessage.setType(type);

    if (LanMsgType.CLIENT == type) {
      String password = ByteStrUtils.readStringByBuf(byteBuf);
      lanMessage.setPassword(password);
    } else if (LanMsgType.CONNECT == type) {
      ByteBuf compressedId = byteBuf.readBytes(requestIdLen);
      lanMessage.setRequestIdByBuf(compressedId);
      String uri = ByteStrUtils.readStringByBuf(byteBuf);
      lanMessage.setUri(uri);
    } else if (LanMsgType.CONNECTED == type) {
      lanMessage.setRequestIdByBuf(byteBuf);
    } else if (LanMsgType.TRANSMIT == type) {
      byte[] data = ByteStrUtils.readArrayByBuf(byteBuf);
      lanMessage.setData(data);
    } else if (LanMsgType.HEARTBEAT == type) {
      lanMessage.setSequenceNumByBuf(byteBuf);
    } else {
      throw new IllegalArgumentException("unsupported msg type: " + type);
    }
    return lanMessage;
  }

}