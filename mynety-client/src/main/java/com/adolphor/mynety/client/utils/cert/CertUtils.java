package com.adolphor.mynety.client.utils.cert;

import com.adolphor.mynety.common.utils.BaseUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;

import static com.adolphor.mynety.client.config.Config.CA_PASSWORD;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
public class CertUtils {

  public static final String preSubject = "L=HangZhou, ST=ZheJiang, C=CN, O=adolphor@qq.com, OU=https://github.com/adolphor/mynety, CN=";

  private static final String signatureAlgorithm = "SHA256WithRSAEncryption";
  private static final String alias = "mynety-cert";

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  public static PrivateKey loadPriKey(String filePath, char[] password) throws Exception {
    KeyStore keyStore = loadKeyStore(filePath, password);
    Enumeration<String> aliasesEnum = keyStore.aliases();
    while (aliasesEnum.hasMoreElements()) {
      String aliases = aliasesEnum.nextElement();
      return (PrivateKey) keyStore.getKey(aliases, password);
    }
    throw new IllegalArgumentException("configuration of caKeyStoreFile is NOT right!");
  }

  public static X509Certificate loadCert(String filePath, char[] password) throws Exception {
    KeyStore keyStore = loadKeyStore(filePath, password);
    Enumeration<String> aliasesEnum = keyStore.aliases();
    while (aliasesEnum.hasMoreElements()) {
      String aliases = aliasesEnum.nextElement();
      return (X509Certificate) keyStore.getCertificate(aliases);
    }
    throw new IllegalArgumentException("configuration of caKeyStoreFile is NOT right!");
  }

  public static X509Certificate genMitmCert(String issuer, PrivateKey caPriKey, Date notBefore, Date notAfter,
                                            PublicKey publicKey, String... hosts) throws Exception {
    String subject = preSubject + hosts[0];
    JcaX509v3CertificateBuilder jv3Builder = new JcaX509v3CertificateBuilder(
        new X500Name(RFC4519Style.INSTANCE, issuer),
        BigInteger.valueOf(System.currentTimeMillis() + BaseUtils.getRandomInt(1000, 9999)),
        notBefore,
        notAfter,
        new X500Name(RFC4519Style.INSTANCE, subject),
        publicKey
    );
    GeneralName[] generalNames = new GeneralName[hosts.length];
    for (int i = 0; i < hosts.length; i++) {
      generalNames[i] = new GeneralName(GeneralName.dNSName, hosts[i]);
    }
    GeneralNames subjectAltName = new GeneralNames(generalNames);
    jv3Builder.addExtension(Extension.subjectAlternativeName, false, subjectAltName);
    ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm).build(caPriKey);
    return new JcaX509CertificateConverter().getCertificate(jv3Builder.build(signer));
  }

  public static KeyPair genKeyPair() throws NoSuchProviderException, NoSuchAlgorithmException {
    KeyPairGenerator caKeyPairGen = KeyPairGenerator.getInstance("RSA", "BC");
    caKeyPairGen.initialize(2048, new SecureRandom());
    return caKeyPairGen.genKeyPair();
  }

  private static KeyStore loadKeyStore(String filePath, char[] password) throws Exception {
    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    FileInputStream inputStream;
    try {
      inputStream = new FileInputStream(filePath);
    } catch (FileNotFoundException e) {
      filePath = Thread.currentThread().getContextClassLoader().getResource("").getPath() + filePath;
      try {
        inputStream = new FileInputStream(filePath);
      } catch (FileNotFoundException e1) {
        throw e1;
      }
    }
    ks.load(inputStream, password);
    return ks;
  }

  /**
   * Generate the CA root cert
   *
   * @param subject
   * @param notBefore
   * @param notAfter
   * @param keyPair
   * @return
   * @throws Exception
   */
  public static X509Certificate genCACert(String subject, Date notBefore, Date notAfter, KeyPair keyPair) throws Exception {
    JcaX509v3CertificateBuilder jv3Builder = new JcaX509v3CertificateBuilder(
        new X500Name(RFC4519Style.INSTANCE, subject),
        BigInteger.valueOf(System.currentTimeMillis() + BaseUtils.getRandomInt(1000, 9999)),
        notBefore,
        notAfter,
        new X500Name(RFC4519Style.INSTANCE, subject),
        keyPair.getPublic()
    );
    jv3Builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));
    ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm).build(keyPair.getPrivate());
    return new JcaX509CertificateConverter().getCertificate(jv3Builder.build(signer));
  }

  public static void saveCertToFile(Certificate cert, String fileName) throws Exception {
    Files.write(Paths.get(fileName), cert.getEncoded());
  }

  public static void saveKeyStoreToFile(Certificate cert, String storeType, KeyPair keyPair, String fileName) throws Exception {
    KeyStore store = KeyStore.getInstance(storeType);
    store.load(null, null);
    store.setKeyEntry(alias, keyPair.getPrivate(), CA_PASSWORD.toCharArray(), new Certificate[]{cert});
    cert.verify(keyPair.getPublic());
    store.store(new FileOutputStream(fileName), CA_PASSWORD.toCharArray());
  }

}
