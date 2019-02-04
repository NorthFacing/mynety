package com.adolphor.mynety.client.utils.cert;

import io.netty.handler.ssl.SslContext;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

public class HttpsCertConfig {

  private SslContext clientSslCtx;
  // CA证书使用者信息
  private String issuer;
  // CA证书有效时段(超出CA有效期，会提示证书不安全)
  private Date caNotBefore;
  private Date caNotAfter;
  // CA私钥用于给动态生成的网站SSL证书签证
  private PrivateKey caPrivateKey;
  // 生产一对随机公私钥用于网站SSL证书动态创建
  private PrivateKey privateKey;
  private PublicKey publicKey;
  private boolean handleSsl;

  public SslContext getClientSslCtx() {
    return clientSslCtx;
  }

  public void setClientSslCtx(SslContext clientSslCtx) {
    this.clientSslCtx = clientSslCtx;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public Date getCaNotBefore() {
    return caNotBefore;
  }

  public void setCaNotBefore(Date caNotBefore) {
    this.caNotBefore = caNotBefore;
  }

  public Date getCaNotAfter() {
    return caNotAfter;
  }

  public void setCaNotAfter(Date caNotAfter) {
    this.caNotAfter = caNotAfter;
  }

  public PrivateKey getCaPrivateKey() {
    return caPrivateKey;
  }

  public void setCaPrivateKey(PrivateKey caPrivateKey) {
    this.caPrivateKey = caPrivateKey;
  }

  public PrivateKey getPrivateKey() {
    return privateKey;
  }

  public void setPrivateKey(PrivateKey privateKey) {
    this.privateKey = privateKey;
  }

  public PublicKey getPublicKey() {
    return publicKey;
  }

  public void setPublicKey(PublicKey publicKey) {
    this.publicKey = publicKey;
  }

}
