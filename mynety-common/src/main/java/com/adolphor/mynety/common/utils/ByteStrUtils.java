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
    return Unpooled.directBuffer().writeBytes(arr);
  }

  public static String getStringByHeapBuf(ByteBuf buf) {
    return new String(buf.array(), StandardCharsets.UTF_8);
  }

  public static String getStringByDirectBuf(ByteBuf buf) {
    return new String(getArrByDirectBuf(buf), StandardCharsets.UTF_8);
  }

  public static byte[] getArrByDirectBuf(ByteBuf buf) {
    byte[] bytes = new byte[buf.readableBytes()];
    buf.readBytes(bytes);
    return bytes;
  }

}
