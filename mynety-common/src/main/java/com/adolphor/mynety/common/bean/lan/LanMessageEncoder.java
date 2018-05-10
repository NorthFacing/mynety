package com.adolphor.mynety.common.bean.lan;

import com.adolphor.mynety.common.utils.ByteStrUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 加密
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.5
 */
@Slf4j
public class LanMessageEncoder extends MessageToByteEncoder<LanMessage> {

  /**
   * 4       +  1   +   8   +     4      +     L     +    4   +  M  +  N
   * 消息长度 + 类型 + 流水号 + 请求来源长度 + 请求来源ID + URI长度 + URI + 正式数据
   *
   * @param ctx
   * @param msg
   * @param out
   * @throws Exception
   */
  @Override
  protected void encode(ChannelHandlerContext ctx, LanMessage msg, ByteBuf out) throws Exception {

    logger.debug("[ {} ] [LanMessageEncoder-encode] received msg: {}", ctx.channel(), msg);

    int frameLength = LanMessage.HEADER_SIZE;
    if (msg.getUri() != null) {
      frameLength += ByteStrUtils.getByteArr(msg.getUri()).length;
    }
    if (msg.getRequestId() != null) {
      frameLength += ByteStrUtils.getByteArr(msg.getRequestId()).length;
    }
    if (msg.getData() != null) {
      frameLength += msg.getData().length;
    }

    out.writeInt(frameLength);
    out.writeByte(msg.getType());
    out.writeLong(msg.getSerialNumber());
    if (StringUtils.isNotEmpty(msg.getRequestId())) {
      ByteBuf userId = ByteStrUtils.getByteBuf(msg.getRequestId());
      out.writeInt(userId.readableBytes());
      out.writeBytes(userId);
    } else {
      out.writeInt(0);
    }
    if (StringUtils.isNotEmpty(msg.getUri())) {
      ByteBuf uri = ByteStrUtils.getByteBuf(msg.getUri());
      out.writeInt(uri.readableBytes());
      out.writeBytes(uri);
    } else {
      out.writeInt(0);
    }
    if (msg.getData() != null) {
      out.writeBytes(msg.getData());
    }

    logger.debug("[ {} ] [LanMessageEncoder-encode] encoded msg: {} bytes", ctx.channel(), out.readableBytes());
  }
}