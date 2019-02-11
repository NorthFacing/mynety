package com.adolphor.mynety.client.utils.cert;

import io.netty.handler.ssl.SslContext;
import lombok.Data;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Data
public class CertConfig {

  private SslContext clientSslCtx;
  private String issuer;
  private Date notBefore;
  private Date notAfter;
  private PrivateKey caPriKey;
  private PrivateKey mitmPriKey;
  private PublicKey mitmPubKey;

}
