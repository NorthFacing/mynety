package com.adolphor.mynety.common.encryption.impl.demo.demo02;

import com.sun.org.apache.xml.internal.security.utils.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * https://blog.csdn.net/c5113620/article/details/82873375
 */
public class CFB_Encrypt {
  public static void main(String[] args) {
    try {
      KeyGenerator kg = KeyGenerator.getInstance("AES");
      //初始化密钥生成器，AES要求密钥长度为128位、192位、256位
      kg.init(128);
      SecretKey secretKey = kg.generateKey();
      System.out.println("密钥：" + Base64.encode(secretKey.getEncoded()));

      SecretKey key = new SecretKeySpec(secretKey.getEncoded(), "AES");

      String txt = "testtxt-AES/ECB/PKCS5Padding";

      Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, key);
      byte[] encryptData = cipher.doFinal(txt.getBytes());
      System.out.println("加密后：" + Base64.encode(encryptData));

      cipher.init(Cipher.DECRYPT_MODE, key);
      byte[] decryptData = cipher.doFinal(encryptData);
      System.out.println("解密后：" + new String(decryptData));

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
