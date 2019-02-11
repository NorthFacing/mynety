package com.adolphor.mynety.common.encryption;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;

/**
 * @author Bob.Zhu
 * @author zhaohui
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
public interface ICrypt {

  ByteBuf encrypt(ByteBuf data) throws IOException, InvalidAlgorithmParameterException;

  byte[] encryptToArray(ByteBuf data) throws IOException, InvalidAlgorithmParameterException;

  ByteBuf decrypt(ByteBuf data) throws IOException, InvalidAlgorithmParameterException;

  byte[] decryptToArray(ByteBuf data) throws IOException, InvalidAlgorithmParameterException;

}
