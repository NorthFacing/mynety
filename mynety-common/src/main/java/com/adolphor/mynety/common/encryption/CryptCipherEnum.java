package com.adolphor.mynety.common.encryption;

import java.security.InvalidAlgorithmParameterException;

import static com.adolphor.mynety.common.constants.Constants.CRYPT_TYPE_AES;
import static com.adolphor.mynety.common.constants.Constants.CRYPT_TYPE_BF;
import static com.adolphor.mynety.common.constants.Constants.CRYPT_TYPE_CL;
import static com.adolphor.mynety.common.constants.Constants.CRYPT_TYPE_SD;
import static com.adolphor.mynety.common.constants.Constants.ENCRYPT_NONE;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.6
 */
public enum CryptCipherEnum {

  // NONE: do not encrypt/decrypt
  NONE("none", ENCRYPT_NONE, 0, 0),

  // AES
  AES_128_CFB("aes-128-cfb", CRYPT_TYPE_AES, 16, 16),
  AES_192_CFB("aes-192-cfb", CRYPT_TYPE_AES, 16, 24),
  AES_256_CFB("aes-256-cfb", CRYPT_TYPE_AES, 16, 32),
  AES_128_OFB("aes-128-ofb", CRYPT_TYPE_AES, 16, 16),
  AES_192_OFB("aes-192-ofb", CRYPT_TYPE_AES, 16, 24),
  AES_256_OFB("aes-256-ofb", CRYPT_TYPE_AES, 16, 32),

  // BF
  BLOWFISH_CFB("bf-cfb", CRYPT_TYPE_BF, 8, 16),

  // CL
  CAMELLIA_128_CFB("camellia-128-cfb", CRYPT_TYPE_CL, 16, 16),
  CAMELLIA_192_CFB("camellia-192-cfb", CRYPT_TYPE_CL, 16, 24),
  CAMELLIA_256_CFB("camellia-256-cfb", CRYPT_TYPE_CL, 16, 32),

  // SD
  SEED_CFB("seed-cfb", CRYPT_TYPE_SD, 16, 16);

  public String cryptName;
  public String cryptType;
  public int ivLength;
  public int keyLength;

  CryptCipherEnum(String cryptName, String cryptType, int ivLength, int keyLength) {
    this.cryptName = cryptName;
    this.cryptType = cryptType;
    this.ivLength = ivLength;
    this.keyLength = keyLength;
  }

  public static CryptCipherEnum getByCryptName(String cryptName) throws InvalidAlgorithmParameterException {
    CryptCipherEnum[] values = CryptCipherEnum.values();
    for (CryptCipherEnum crypt : values) {
      if (crypt.cryptName.equals(cryptName)) {
        return crypt;
      }
    }
    throw new InvalidAlgorithmParameterException("Unsupported crypt type: " + cryptName);
  }

}
