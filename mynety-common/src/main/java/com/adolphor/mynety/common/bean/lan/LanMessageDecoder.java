package com.adolphor.mynety.common.bean.lan;

import com.adolphor.mynety.common.utils.ByteStrUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.bean.lan.LanMessage.FRAME_SIZE;
import static com.adolphor.mynety.common.bean.lan.LanMessage.HEADER_SIZE;
import static com.adolphor.mynety.common.constants.LanConstants.INITIAL_BYTES_TO_STRIP;
import static com.adolphor.mynety.common.constants.LanConstants.LENGTH_ADJUSTMENT;
import static com.adolphor.mynety.common.constants.LanConstants.LENGTH_FIELD_LENGTH;
import static com.adolphor.mynety.common.constants.LanConstants.LENGTH_FIELD_OFFSET;
import static com.adolphor.mynety.common.constants.LanConstants.MAX_FRAME_LENGTH;

/**
 * 解密
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.5
 */
@Slf4j
public class LanMessageDecoder extends LengthFieldBasedFrameDecoder {

  public LanMessageDecoder() {
    super(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP);
  }

  /**
   * 4       +  1   +   8   +     4      +     L     +    4   +  M  +  N
   * 消息长度 + 类型 + 流水号 + 请求来源长度 + 请求来源ID + URI长度 + URI + 正式数据
   *
   * @param ctx
   * @param msg
   * @return
   * @throws Exception
   */
  @Override
  protected LanMessage decode(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    logger.debug("[ {} ] [LanMessageDecoder-decode] received msg: {} bytes", ctx.channel(), msg.readableBytes());
    ByteBuf byteBuf = (ByteBuf) super.decode(ctx, msg);
    if (byteBuf == null) {
      return null;
    }
    if (byteBuf.readableBytes() < HEADER_SIZE) {
      return null;
    }
    int frameLength = byteBuf.readInt();
    if (byteBuf.readableBytes() < (frameLength - FRAME_SIZE)) {
      return null;
    }
    byte type = byteBuf.readByte();
    long sn = byteBuf.readLong();
    int srcLen = byteBuf.readInt();
    String userId = null;
    if (srcLen > 0) {
      userId = ByteStrUtils.getString(byteBuf, srcLen);
    }
    int uriLen = byteBuf.readInt();
    String uri = null;
    if (uriLen > 0) {
      uri = ByteStrUtils.getString(byteBuf, uriLen);
    }
    int dataLen = frameLength - (HEADER_SIZE + srcLen + uriLen);
    byte[] data = null;
    if (dataLen > 0) {
      data = ByteStrUtils.getByteArr(byteBuf, dataLen);
    }

    LanMessage lanMessage = new LanMessage();
    lanMessage.setType(type);
    lanMessage.setSerialNumber(sn);
    lanMessage.setRequestId(userId);
    lanMessage.setUri(uri);
    lanMessage.setData(data);

    byteBuf.release();

    logger.debug("[ {} ] [LanMessageDecoder-decode] decoded msg: {}", ctx.channel(), lanMessage);

    return lanMessage;
  }

}