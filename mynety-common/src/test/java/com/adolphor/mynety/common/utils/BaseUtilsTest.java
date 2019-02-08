package com.adolphor.mynety.common.utils;

import org.junit.Test;

import static com.adolphor.mynety.common.utils.BaseUtils.compressUUID;
import static com.adolphor.mynety.common.utils.BaseUtils.deCompressUUID;
import static org.junit.Assert.assertEquals;

public class BaseUtilsTest {

  @Test
  public void getUUID() {
  }

  /**
   * 1 Byte：byte，boolean   字节数据，逻辑变量(true,flase)
   * 2 Byte：short，char     短整数，一个字符
   * 4 Byte：int，float      普通整数，浮点数
   * 8 Byte：long，double    长整数，双精度浮点数
   */
  @Test
  public void base() {

    short aShort = Short.valueOf(String.valueOf('1'), 16);
    assertEquals(1, aShort);
    assertEquals("00000000000000000000000000000001", ByteUtils.bitStr(aShort));

    int asf = aShort << 4;
    assertEquals(16, asf);
    assertEquals("00000000000000000000000000010000", ByteUtils.bitStr(asf));

    short bShort = Short.valueOf(String.valueOf('d'), 16);
    assertEquals(13, bShort);
    assertEquals("00000000000000000000000000001101", ByteUtils.bitStr(bShort));

    int sum = asf | bShort;
    assertEquals(29, sum);
    assertEquals("00000000000000000000000000011101", ByteUtils.bitStr(sum));
    char charSUM = (char) sum;
    assertEquals("00000000000000000000000000011101", ByteUtils.bitStr(charSUM));

    int high = (sum & 0xff) >> 4;
    assertEquals(1, high);
    assertEquals("00000000000000000000000000000001", ByteUtils.bitStr(high));
    assertEquals("1", Integer.toHexString(high));
    int low = sum & 0x0f;
    assertEquals(13, low);
    assertEquals("00000000000000000000000000001101", ByteUtils.bitStr(low));
    assertEquals("d", Integer.toHexString(low));

  }

  @Test
  public void compressAndDeCompressUUID() {
    for (int i = 0; i < 100; i++) {
      String uuid = BaseUtils.getUUID();
      byte[] comUuid = compressUUID(uuid);
      String du = deCompressUUID(comUuid);
      assertEquals(uuid, du);
    }
  }

}