package com.adolphor.mynety.common.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

/**
 * ByteBuf 和 String 转换工具类
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
public class ByteStrUtils {

  public static ByteBuf getDirectBuf(byte[] arr) {
    return Unpooled.directBuffer(arr.length).writeBytes(arr);
  }

  public static ByteBuf getHeapBuf(byte[] arr) {
    return Unpooled.buffer(arr.length).writeBytes(arr);
  }

  public static String getStringByBuf(ByteBuf buf) {
    return new String(getArrayByBuf(buf), StandardCharsets.UTF_8);
  }

  public static byte[] getArrayByBuf(ByteBuf buf) {
    if (buf.hasArray()) {
      byte[] array = buf.array();
      int offset = buf.arrayOffset() + buf.readerIndex();
      int length = buf.readableBytes();
      // for good performance, reduce copy time
      if (length + offset == array.length) {
        return array;
      }
      byte[] temp = new byte[length];
      System.arraycopy(array, offset, temp, 0, length);
      return temp;
    } else {
      int length = buf.readableBytes();
      byte[] array = new byte[length];
      buf.getBytes(buf.readerIndex(), array);
      return array;
    }
  }

}
