package com.adolphor.mynety.common.bean.lan;

import com.adolphor.mynety.common.constants.LanMsgType;
import com.adolphor.mynety.common.utils.BaseUtils;
import com.adolphor.mynety.common.utils.ByteStrUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

import static com.adolphor.mynety.common.bean.lan.LanMessage.HEADER_SIZE;
import static com.adolphor.mynety.common.constants.LanConstants.INITIAL_BYTES_TO_STRIP;
import static com.adolphor.mynety.common.constants.LanConstants.LENGTH_ADJUSTMENT;
import static com.adolphor.mynety.common.constants.LanConstants.LENGTH_FIELD_LENGTH;
import static com.adolphor.mynety.common.constants.LanConstants.LENGTH_FIELD_OFFSET;
import static com.adolphor.mynety.common.constants.LanConstants.MAX_FRAME_LENGTH;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * 解密
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
public class LanMessageDecoder extends LengthFieldBasedFrameDecoder {

  public LanMessageDecoder() {
    super(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP);
  }

  /**
   * 4       +  1   +   8   +          16           +  DATA
   * 消息长度 + 类型 + 流水号 + 请求来源ID(压缩后的UUID) + 数据内容
   *
   * @param ctx
   * @param msg
   * @return
   * @throws Exception
   */
  @Override
  protected LanMessage decode(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    logger.debug("[ {} ]【{}】待解码处理的 msg 信息: {} bytes", ctx.channel().id(), getSimpleName(this), msg.readableBytes());
    ByteBuf byteBuf = (ByteBuf) super.decode(ctx, msg);
    if (byteBuf == null) {
      return null;
    }
    if (byteBuf.readableBytes() < HEADER_SIZE) {
      return null;
    }
    int frameLength = byteBuf.readInt();
    if (byteBuf.readableBytes() < (frameLength - 4)) {
      logger.warn("incorrect msg length...");
      return null;
    }
    byte type = byteBuf.readByte();
    LanMsgType lanMsgType = LanMsgType.getType(type);

    long sn = byteBuf.readLong();

    ByteBuf reqIdBuf = byteBuf.readBytes(16);
    byte[] reqIdArr = ByteStrUtils.getArrByDirectBuf(reqIdBuf);
    String requestId = BaseUtils.deCompressUUID(reqIdArr);

    String uri = null;
    byte[] data = null;
    if (LanMsgType.CONNECT == lanMsgType) {
      byte[] uriArr = ByteStrUtils.getArrByDirectBuf(byteBuf);
      uri = new String(uriArr, StandardCharsets.UTF_8);
    } else {
      data = ByteStrUtils.getArrByDirectBuf(byteBuf);
    }

    LanMessage lanMessage = new LanMessage();

    lanMessage.setType(lanMsgType);
    lanMessage.setSerialNumber(sn);
    lanMessage.setRequestId(requestId);
    lanMessage.setUri(uri);
    lanMessage.setData(data);

    logger.debug("[ {} ]【{}】解码处理之后的 msg 信息: {}", ctx.channel().id(), getSimpleName(this), lanMessage);
    return lanMessage;
  }

}