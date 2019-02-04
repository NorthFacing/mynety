package com.adolphor.mynety.client.utils.cert;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CertUtils {

  private static final String preSub = "L=HangZhou, ST=ZheJiang, C=CN, OU=https://github.com/adolphor/mynety, O=Bob.Zhu, E=adolphor@qq.com, CN=";
  private static final String signatureAlgorithm = "SHA256WithRSAEncryption";

  static {
    //注册BouncyCastleProvider加密库
    Security.addProvider(new BouncyCastleProvider());
  }

  private static KeyFactory keyFactory = null;

  private static KeyFactory getKeyFactory() throws NoSuchAlgorithmException {
    if (keyFactory == null) {
      keyFactory = KeyFactory.getInstance("RSA");
    }
    return keyFactory;
  }

  /**
   * 从文件加载RSA私钥 openssl pkcs8 -topk8 -nocrypt -inform PEM -outform DER -in ca.key -out
   * ca_private.der
   */
  public static PrivateKey loadPriKey(InputStream inputStream) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    byte[] bts = new byte[1024];
    int len;
    while ((len = inputStream.read(bts)) != -1) {
      outputStream.write(bts, 0, len);
    }
    inputStream.close();
    outputStream.close();
    return loadPriKey(outputStream.toByteArray());
  }

  /**
   * 从文件加载RSA私钥 openssl pkcs8 -topk8 -nocrypt -inform PEM -outform DER -in ca.key -out
   * ca_private.der
   */
  public static PrivateKey loadPriKey(byte[] bts) throws NoSuchAlgorithmException, InvalidKeySpecException {
    EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(bts);
    return getKeyFactory().generatePrivate(privateKeySpec);
  }

  /**
   * 从文件加载证书
   */
  public static X509Certificate loadCert(InputStream inputStream) throws CertificateException {
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    return (X509Certificate) cf.generateCertificate(inputStream);
  }

  /**
   * 读取ssl证书使用者信息
   */
  public static String getSubject(X509Certificate certificate) throws Exception {
    //读出来顺序是反的需要反转下
    List<String> tempList = Arrays.asList(certificate.getIssuerDN().toString().split(", "));
    return IntStream.rangeClosed(0, tempList.size() - 1)
        .mapToObj(i -> tempList.get(tempList.size() - i - 1)).collect(Collectors.joining(", "));
  }

  /**
   * 根据CA证书subject来动态生成目标服务器证书，并进行CA签授
   *
   * @param issuer 颁发机构
   */
  public static X509Certificate genCert(String issuer, PrivateKey caPriKey, Date caNotBefore, Date caNotAfter,
                                        PublicKey serverPubKey, String... hosts) throws Exception {
    String subject = preSub + hosts[0];
    JcaX509v3CertificateBuilder jv3Builder = new JcaX509v3CertificateBuilder(
        new X500Name(issuer),
        // 修复ElementaryOS上证书不安全问题(serialNumber为1时证书会提示不安全)，避免serialNumber冲突，采用时间戳+4位随机数生成
        BigInteger.valueOf(System.currentTimeMillis() + (long) (Math.random() * 10000) + 1000),
        caNotBefore,
        caNotAfter,
        new X500Name(subject),
        serverPubKey
    );
    //SAN扩展证书支持的域名，否则浏览器提示证书不安全
    GeneralName[] generalNames = new GeneralName[hosts.length];
    for (int i = 0; i < hosts.length; i++) {
      generalNames[i] = new GeneralName(GeneralName.dNSName, hosts[i]);
    }
    GeneralNames subjectAltName = new GeneralNames(generalNames);
    jv3Builder.addExtension(Extension.subjectAlternativeName, false, subjectAltName);
    //SHA256 用SHA1浏览器可能会提示证书不安全
    ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm).build(caPriKey);
    return new JcaX509CertificateConverter().getCertificate(jv3Builder.build(signer));
  }

  /**
   * 生成CA服务器根证书
   */
  public static X509Certificate genCACert(String subject, Date caNotBefore, Date caNotAfter, KeyPair keyPair)
      throws Exception {
    JcaX509v3CertificateBuilder jv3Builder = new JcaX509v3CertificateBuilder(
        new X500Name(subject),
        BigInteger.valueOf(System.currentTimeMillis() + (long) (Math.random() * 10000) + 1000),
        caNotBefore,
        caNotAfter,
        new X500Name(subject),
        keyPair.getPublic()
    );
    jv3Builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));
    ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm).build(keyPair.getPrivate());
    return new JcaX509CertificateConverter().getCertificate(jv3Builder.build(signer));
  }

  /**
   * 生成RSA公私密钥对，长度为2048
   */
  public static KeyPair genKeyPair() throws Exception {
    KeyPairGenerator caKeyPairGen = KeyPairGenerator.getInstance("RSA", "BC");
    caKeyPairGen.initialize(2048, new SecureRandom());
    return caKeyPairGen.genKeyPair();
  }

  /**
   * 只需要运行main方法生成一次，导入到系统根证书列表，然后设置为信任即可
   *
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    KeyPair keyPair = CertUtils.genKeyPair();
    File caCertFile = new File("mynety-client/src/main/resources/mynety-root-ca.crt");
    if (caCertFile.exists()) {
      caCertFile.delete();
    }
    Files.write(Paths.get(caCertFile.toURI()),
        CertUtils.genCACert(
            preSub + "mynety Root CA",
            new Date(),
            new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3650)),
            keyPair
        ).getEncoded());

    File caKeyFile = new File("mynety-client/src/main/resources/mynety-root-ca-private-key.der");
    if (caKeyFile.exists()){
      caKeyFile.delete();
    }
    Files.write(Paths.get(caKeyFile.toURI()),keyPair.getPrivate().getEncoded());
  }

}
