package com.adolphor.mynety.common.encryption;

import com.adolphor.mynety.common.utils.ByteStrUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.StreamBlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.engines.BlowfishEngine;
import org.bouncycastle.crypto.engines.CamelliaEngine;
import org.bouncycastle.crypto.engines.SEEDEngine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;
import org.bouncycastle.crypto.modes.OFBBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.adolphor.mynety.common.constants.Constants.CRYPT_TYPE_AES;
import static com.adolphor.mynety.common.constants.Constants.CRYPT_TYPE_AES_OFB;
import static com.adolphor.mynety.common.constants.Constants.CRYPT_TYPE_BF;
import static com.adolphor.mynety.common.constants.Constants.CRYPT_TYPE_CL;
import static com.adolphor.mynety.common.constants.Constants.CRYPT_TYPE_SD;

/**
 * @author Bob.Zhu
 * @author zhaohui
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
public class CryptImpl implements ICrypt {

  private final Lock encLock = new ReentrantLock();
  private final Lock decLock = new ReentrantLock();

  private final SecretKey key;
  private final CryptCipherEnum cipherInfo;

  private boolean isEncryptIVSet;
  private boolean isDecryptIVSet;
  private StreamBlockCipher encCipher;
  private StreamBlockCipher decCipher;

  public CryptImpl(String password, CryptCipherEnum cipherInfo) {
    this.cipherInfo = cipherInfo;
    if (cipherInfo == CryptCipherEnum.NONE) {
      key = null;
    } else {
      ShadowSocksKey ssKey = new ShadowSocksKey(password, cipherInfo.keyLength);
      this.key = new SecretKeySpec(ssKey.getEncoded(), cipherInfo.cryptType);
    }
  }

  @Override
  public byte[] encryptToArray(ByteBuf data) throws IOException, InvalidAlgorithmParameterException {
    if (cipherInfo == CryptCipherEnum.NONE) {
      return ByteStrUtils.readArrayByBuf(data);
    }
    try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
      synchronized (encLock) {
        stream.reset();
        if (!isEncryptIVSet) {
          byte[] iv = randomBytes(cipherInfo.ivLength);
          stream.write(iv);
          ParametersWithIV parameterIV = new ParametersWithIV(new KeyParameter(key.getEncoded()), iv);
          encCipher = getCipher();
          encCipher.init(true, parameterIV);
          isEncryptIVSet = true;
        }
        byte[] array = ByteStrUtils.readArrayByBuf(data);
        processData(encCipher, array, stream);
      }
      return stream.toByteArray();
    } catch (Exception e) {
      throw e;
    }
  }

  @Override
  public ByteBuf encrypt(ByteBuf data) throws IOException, InvalidAlgorithmParameterException {
    return Unpooled.wrappedBuffer(encryptToArray(data));
  }

  @Override
  public byte[] decryptToArray(ByteBuf data) throws IOException, InvalidAlgorithmParameterException {
    if (cipherInfo == CryptCipherEnum.NONE) {
      return ByteStrUtils.readArrayByBuf(data);
    }
    try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
      synchronized (decLock) {
        stream.reset();
        if (!isDecryptIVSet) {
          decCipher = getCipher();
          byte[] iv = ByteStrUtils.readArrayByBuf(data.readBytes(cipherInfo.ivLength));
          ParametersWithIV parameterIV = new ParametersWithIV(new KeyParameter(key.getEncoded()), iv);
          decCipher.init(false, parameterIV);
          isDecryptIVSet = true;
        }
        byte[] array = ByteStrUtils.readArrayByBuf(data);
        processData(decCipher, array, stream);
      }
      return stream.toByteArray();
    } catch (Exception e) {
      throw e;
    }
  }

  @Override
  public ByteBuf decrypt(ByteBuf data) throws IOException, InvalidAlgorithmParameterException {
    return Unpooled.wrappedBuffer(decryptToArray(data));
  }

  private void processData(StreamBlockCipher cipher, byte[] data, ByteArrayOutputStream stream) {
    byte[] buffer = new byte[data.length];
    int noBytesProcessed = cipher.processBytes(data, 0, data.length, buffer, 0);
    stream.write(buffer, 0, noBytesProcessed);
  }

  private byte[] randomBytes(int size) {
    byte[] bytes = new byte[size];
    new SecureRandom().nextBytes(bytes);
    return bytes;
  }

  private StreamBlockCipher getCipher() throws InvalidAlgorithmParameterException {
    StreamBlockCipher cipher;
    switch (cipherInfo.cryptType) {
      case CRYPT_TYPE_AES:
        AESEngine aesEngine = new AESEngine();
        if (cipherInfo.cryptName.endsWith(CRYPT_TYPE_AES_OFB)) {
          cipher = new OFBBlockCipher(aesEngine, cipherInfo.ivLength * 8);
        } else {
          cipher = new CFBBlockCipher(aesEngine, cipherInfo.ivLength * 8);
        }
        break;
      case CRYPT_TYPE_BF:
        BlowfishEngine bfEngine = new BlowfishEngine();
        cipher = new CFBBlockCipher(bfEngine, cipherInfo.ivLength * 8);
        break;
      case CRYPT_TYPE_CL:
        CamelliaEngine clEngine = new CamelliaEngine();
        cipher = new CFBBlockCipher(clEngine, cipherInfo.ivLength * 8);
        break;
      case CRYPT_TYPE_SD:
        SEEDEngine sdEngine = new SEEDEngine();
        cipher = new CFBBlockCipher(sdEngine, cipherInfo.ivLength * 8);
        break;
      default:
        throw new InvalidAlgorithmParameterException("Unsupported crypt type: " + cipherInfo.cryptType);
    }
    return cipher;
  }

}
