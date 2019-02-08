package com.adolphor.mynety.common.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * ByteBuf 和 String 转换工具类
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
public class ByteStrUtils {

  /**
   * 获取 byte[] 数组
   *
   * @param buf
   * @return
   */
  public static byte[] getByteArr(ByteBuf buf) {
    if (buf == null || buf.readableBytes() < 1) {
      return null;
    }
    return getByteArr(buf, buf.readableBytes());
  }

  public static byte[] getByteArr(ByteBuf buf, int len) {
    if (buf == null || buf.readableBytes() < 1) {
      return null;
    }
    byte[] bytes = new byte[len];
    buf.readBytes(bytes);
    return bytes;
  }

  public static byte[] getByteArr(String str) {
    if (StringUtils.isEmpty(str)) {
      return null;
    }
    return str.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * 获取 ByteBuf
   *
   * @param str
   * @return
   */
  public static ByteBuf getByteBuf(String str) {
    if (StringUtils.isEmpty(str)) {
      return null;
    }
    byte[] bytes = getByteArr(str);
    return Unpooled.wrappedBuffer(bytes);
  }

  public static ByteBuf getDirectByteBuf(byte[] arr) {
    if (arr == null) {
      return null;
    }
    return Unpooled.directBuffer().writeBytes(arr);
  }

  public static ByteBuf getByteBuf(byte[] arr) {
    if (arr == null) {
      return null;
    }
    return Unpooled.wrappedBuffer(arr);
  }

  /**
   * 获取 String
   * use <code>ByteBufUtil.hexDump(buf)</code> instead
   * @param buf
   * @return
   */
  @Deprecated
  public static String getString(ByteBuf buf) {
    return getString(buf, buf.readableBytes());
  }

  @Deprecated
  public static String getString(ByteBuf buf, int len) {
    if (buf == null || buf.readableBytes() < 1) {
      return null;
    }
    byte[] byteArr = getByteArr(buf, len);
    return new String(byteArr, StandardCharsets.UTF_8);
  }

  @Deprecated
  public static String getString(byte[] arr) {
    if (arr == null) {
      return null;
    }
    return new String(arr, StandardCharsets.UTF_8);
  }

  /**
   * 测试
   *
   * @param args
   */
  public static void main(String[] args) {

    String uuid = UUID.randomUUID().toString().replace("-", "");
    System.out.println(uuid);

    byte[] byteArr = getByteArr(uuid);
    System.out.println(byteArr.length);

    ByteBuf byteBuf = getByteBuf(uuid);
    System.out.println(byteBuf.readableBytes());

    String string = getString(byteBuf);
    System.out.println(string);

  }

}
