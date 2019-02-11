package com.adolphor.mynety.common.encryption;

import lombok.extern.slf4j.Slf4j;

import java.security.InvalidAlgorithmParameterException;

/**
 * @author Bob.Zhu
 * @author zhaohui
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
public class CryptFactory {

  public static ICrypt get(String cryptName, String password) throws InvalidAlgorithmParameterException {
    CryptCipherEnum info = CryptCipherEnum.getByCryptName(cryptName);
    CryptImpl crypt = new CryptImpl(password, info);
    return crypt;
  }

}
