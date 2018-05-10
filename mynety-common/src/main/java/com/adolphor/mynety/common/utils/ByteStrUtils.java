package com.adolphor.mynety.common.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * ByteBuf 和 String 转换工具类
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.5
 */
public class ByteStrUtils {

  public static String getUUID() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  public static byte[] getByteArr(ByteBuf buf) {
    return getByteArr(buf, buf.readableBytes());
  }

  public static byte[] getByteArr(ByteBuf buf, int len) {
    byte[] bytes = new byte[len];
    buf.readBytes(bytes);
    return bytes;
  }

  public static String getString(ByteBuf buf, int len) {
    byte[] byteArr = getByteArr(buf, len);
    return new String(byteArr, StandardCharsets.UTF_8);
  }

  public static String getString(ByteBuf buf) {
    return getString(buf, buf.readableBytes());
  }

  public static ByteBuf getByteBuf(String str) {
    byte[] bytes = getByteArr(str);
    return Unpooled.wrappedBuffer(bytes);
  }

  public static byte[] getByteArr(String str) {
    return str.getBytes(StandardCharsets.UTF_8);
  }

  public static void main(String[] args) {
    String uuid = getUUID();
    System.out.println(uuid);

    byte[] byteArr = getByteArr(uuid);
    System.out.println(byteArr.length);

    ByteBuf byteBuf = getByteBuf(uuid);
    System.out.println(byteBuf.readableBytes());

    String string = getString(byteBuf);
    System.out.println(string);

  }

}
