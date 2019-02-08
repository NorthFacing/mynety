package com.adolphor.mynety.common.utils;

import java.util.UUID;

/**
 * Java基础工具类
 * <p>
 * <p>
 * 参考：
 * 1. [byte为什么要与上0xff？](https://www.cnblogs.com/think-in-java/p/5527389.html)
 * 2. [如何压缩UUID长度？](http://www.cnblogs.com/smallyard/p/8271082.html)
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
public class BaseUtils {

  public static String getUUID() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  /**
   * uuid压缩算法，利用高低位将32位的UUID压缩为16位。
   * 虽然 tmp 使用的 int 占用4个字节，但这类运算的结果最大也不会超过一个字节内容。
   * 同理，在构造字符串的时候将合并结果转为char只是为了字符串拼接。
   *
   * @param uuid
   * @return
   */
  public static byte[] compressUUID(String uuid) {
    byte[] bytes = new byte[16];
    boolean isFirst = true;
    int tmp = 0;
    for (int i = 0; i < 32; i++) {
      char c = uuid.charAt(i);
      Short aShort = Short.valueOf(String.valueOf(c), 16);
      if (isFirst) {
        tmp = aShort << 4;
        isFirst = false;
      } else {
        bytes[i/2] = (byte) (tmp | aShort);

        isFirst = true;
        tmp = 0;
      }
    }
    return bytes;
  }

  /**
   * 压缩的uuid还原算法：将高低位拆分还原为32位的UUID
   *
   * @param compressedUUID
   * @return
   */
  public static String deCompressUUID(byte[] compressedUUID) {
    StringBuilder resultBuilder = new StringBuilder();
    int high;
    int low;
    for (byte b : compressedUUID) {
      high = (b & 0xff) >> 4;
      low = b & 0x0f;
      resultBuilder.append(Integer.toHexString(high));
      resultBuilder.append(Integer.toHexString(low));
    }
    return resultBuilder.toString();
  }

}
