package com.adolphor.mynety.common.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * utils of ByteBuf & String & array
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
public class ByteStrUtils {

  public static ByteBuf getDirectBuf(byte[] arr) {
    return Unpooled.directBuffer().writeBytes(arr);
  }

  public static ByteBuf getFixedLenDirectBuf(byte[] arr) {
    return Unpooled.directBuffer(arr.length).writeBytes(arr);
  }

  public static ByteBuf getHeapBuf(byte[] arr) {
    return Unpooled.buffer().writeBytes(arr);
  }

  public static ByteBuf getFixedLenHeapBuf(byte[] arr) {
    return Unpooled.buffer(arr.length).writeBytes(arr);
  }

  public static String readStringByBuf(ByteBuf buf) {
    return readStringByBuf(buf, buf.readableBytes());
  }

  public static String readStringByBuf(ByteBuf buf, Integer len) {
    if (buf.readableBytes() < len) {
      return null;
    }
    return new String(readArrayByBuf(buf, len), StandardCharsets.UTF_8);
  }

  public static byte[] readArrayByBuf(ByteBuf buf) {
    if (buf.readableBytes() == 0) {
      return null;
    }
    return readArrayByBuf(buf, null);
  }

  public static byte[] readArrayByBuf(ByteBuf buf, Integer len) {
    ByteBuf tempBuf;
    if (len != null) {
      if (len <= 0 || buf.readableBytes() < len) {
        return null;
      } else {
        tempBuf = buf.readBytes(len);
      }
    } else {
      // at below will execute release method, so retain once, avoid IllegalReferenceCountException
      ReferenceCountUtil.retain(buf);
      tempBuf = buf;
    }
    if (tempBuf.hasArray()) {
      byte[] array = tempBuf.array();
      int offset = tempBuf.arrayOffset() + tempBuf.readerIndex();
      int length = tempBuf.readableBytes();
      byte[] temp = new byte[length];
      System.arraycopy(array, offset, temp, 0, length);
      // avoid resource leak
      tempBuf.release();
      return temp;
    } else {
      int length = tempBuf.readableBytes();
      byte[] array = new byte[length];
      tempBuf.getBytes(tempBuf.readerIndex(), array);
      // avoid resource leak
      tempBuf.release();
      return array;
    }
  }

  public static int getBytesLen(String text) {
    return text.getBytes(StandardCharsets.UTF_8).length;
  }

}
