package com.adolphor.mynety.common.utils;

import java.util.Random;
import java.util.UUID;

/**
 * Java base utils
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.6
 */
public class BaseUtils {

  public static String getUUID() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  /**
   * compress 32 chars UUID to 16 bytes
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
        bytes[i / 2] = (byte) (tmp | aShort);

        isFirst = true;
        tmp = 0;
      }
    }
    return bytes;
  }

  /**
   * decompress 16 bytes UUID to 32 chars
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

  /**
   * Get random int number
   *
   * @param min the minute value of result
   * @param max the max value of result
   * @return random number
   */
  public static int getRandomInt(int min, int max) {
    return new Random().nextInt(max - min) + min;
  }

}
