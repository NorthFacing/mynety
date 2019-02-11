package com.adolphor.mynety.common.encryption;

import com.adolphor.mynety.common.utils.ByteStrUtils;
import io.netty.buffer.ByteBuf;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class CryptUtilTest {

  @Test
  public void encrypt() throws Exception {

    final String METHOD = "aes-256-cfb";
    final String PASSWORD = "123456";

    ICrypt enic = CryptFactory.get(METHOD, PASSWORD);
    ICrypt deic = CryptFactory.get(METHOD, PASSWORD);

    String cryptMsg = "hello encrypt!";

    for (int i = 0; i < 10; i++) {
      cryptMsg += (" => " + 1);
      byte[] bytesMsg = cryptMsg.getBytes(StandardCharsets.UTF_8);
      ByteBuf srcBuf = ByteStrUtils.getDirectBuf(bytesMsg);

      ByteBuf encryptMsg = enic.encrypt(srcBuf);

      ByteBuf decryptMsg = deic.decrypt(encryptMsg);
      String result = new String(ByteStrUtils.getArrayByBuf(decryptMsg), StandardCharsets.UTF_8);

      Assert.assertEquals(cryptMsg, result);
    }
  }

  @Test
  public void decrypt() {
  }
}