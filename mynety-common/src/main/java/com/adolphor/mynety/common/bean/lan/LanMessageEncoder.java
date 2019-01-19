package com.adolphor.mynety.common.bean.lan;

import com.adolphor.mynety.common.utils.ByteStrUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * 加密
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
public class LanMessageEncoder extends MessageToByteEncoder<LanMessage> {

  /**
   * 4       +  1   +   8   +     4      +     L     +    4   +  M  +  N
   * 消息长度 + 类型 + 流水号 + 请求来源长度 + 请求来源ID + URI长度 + URI + 正式数据
   *
   * @param ctx
   * @param lanMessage
   * @param out
   * @throws Exception
   */
  @Override
  protected void encode(ChannelHandlerContext ctx, LanMessage lanMessage, ByteBuf out) throws Exception {

    logger.debug("[ {} ]【{}】待编码处理的 msg 信息: {}", ctx.channel().id(), getSimpleName(this), lanMessage);

    int frameLength = LanMessage.HEADER_SIZE;
    if (lanMessage.getUri() != null) {
      frameLength += ByteStrUtils.getByteArr(lanMessage.getUri()).length;
    }
    if (lanMessage.getRequestId() != null) {
      frameLength += ByteStrUtils.getByteArr(lanMessage.getRequestId()).length;
    }
    if (lanMessage.getData() != null) {
      frameLength += lanMessage.getData().length;
    }

    out.writeInt(frameLength);
    out.writeByte(lanMessage.getType());
    out.writeLong(lanMessage.getSerialNumber());
    if (StringUtils.isNotEmpty(lanMessage.getRequestId())) {
      ByteBuf userId = ByteStrUtils.getByteBuf(lanMessage.getRequestId());
      out.writeInt(userId.readableBytes());
      out.writeBytes(userId);
    } else {
      out.writeInt(0);
    }
    if (StringUtils.isNotEmpty(lanMessage.getUri())) {
      ByteBuf uri = ByteStrUtils.getByteBuf(lanMessage.getUri());
      out.writeInt(uri.readableBytes());
      out.writeBytes(uri);
    } else {
      out.writeInt(0);
    }
    if (lanMessage.getData() != null) {
      logger.debug("[ {} ]【{}】需要编码处理的 data 信息: {} bytes => {}", ctx.channel().id(), getSimpleName(this), lanMessage.getData().length, lanMessage.getData());
      out.writeBytes(lanMessage.getData());
    }

    logger.debug("[ {} ]【{}】编码处理之后的 msg 信息: {} bytes", ctx.channel().id(), getSimpleName(this), out.readableBytes());
  }
}