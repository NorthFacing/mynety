package com.adolphor.mynety.client.utils.cert;

import com.adolphor.mynety.common.utils.CalendarUtils;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;

import static com.adolphor.mynety.client.utils.cert.CertUtils.loadPriKey;
import static com.adolphor.mynety.client.utils.cert.CertUtils.preSubject;
import static com.adolphor.mynety.client.utils.cert.CertUtils.saveCertToFile;
import static com.adolphor.mynety.client.utils.cert.CertUtils.saveKeyStoreToFile;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CertUtilsTest {

  private static final String caCnName = "mynety Root CA";

  private static String keyStoreFile = System.getProperty("user.dir") + "/src/test/resources/mynety-root-ca.jks";
  private static String certFile = System.getProperty("user.dir") + "/src/test/resources/mynety-root-ca.cert";
  private static char[] caPassword = "mynety-ca-password".toCharArray();

  private static final Date notBefore = CalendarUtils.today();
  private static final Date notAfter = CalendarUtils.addYears(notBefore, 100);

  @Test
  public void M01_genKeyPair() throws NoSuchProviderException, NoSuchAlgorithmException {
    KeyPair keyPair = CertUtils.genKeyPair();
    System.out.println(keyPair.getPrivate());
    System.out.println(keyPair.getPublic());
  }

  @Test
  public void M02_genCACert() throws Exception {

    KeyPair keyPair = CertUtils.genKeyPair();
    String subject = preSubject + caCnName;
    X509Certificate caCert = CertUtils.genCACert(subject, notBefore, notAfter, keyPair);

    Assert.assertEquals(preSubject + caCnName, caCert.getSubjectDN().toString());
    Assert.assertEquals(notBefore, caCert.getNotBefore());
    Assert.assertEquals(notAfter, caCert.getNotAfter());

    String storeType = KeyStore.getDefaultType();
    saveKeyStoreToFile(caCert, storeType, keyPair, keyStoreFile);
    Assert.assertTrue(new File(keyStoreFile).exists());

    saveCertToFile(caCert, certFile);
    Assert.assertTrue(new File(certFile).exists());
  }

  @Test
  public void M03_loadCert() throws Exception {
    X509Certificate caCert = CertUtils.loadCert(keyStoreFile, caPassword);
    Assert.assertEquals(preSubject + caCnName, caCert.getSubjectDN().toString());

    Date notBefore = CalendarUtils.today();
    Date notAfter = CalendarUtils.addYears(notBefore, 100);

    Assert.assertEquals(notBefore, caCert.getNotBefore());
    Assert.assertEquals(notAfter, caCert.getNotAfter());
  }

  @Test
  public void M04_loadPriKey() throws Exception {
    PrivateKey privateKey = CertUtils.loadPriKey(keyStoreFile, caPassword);
    System.out.println(privateKey.toString());
  }

  @Test
  public void M05_genMitmCert() throws Exception {
    X509Certificate caCert = CertUtils.loadCert(keyStoreFile, caPassword);
    PrivateKey privateKey = loadPriKey(keyStoreFile, caPassword);
    String issuer = caCert.getIssuerDN().toString();
    String subject = "adolphor.com";
    X509Certificate mitmCert = CertUtils.genMitmCert(issuer, privateKey, notBefore, notAfter, caCert.getPublicKey(), subject);

    Assert.assertEquals(preSubject + subject, mitmCert.getSubjectDN().toString());
    Assert.assertEquals(notAfter, mitmCert.getNotAfter());
    Assert.assertEquals(notBefore, mitmCert.getNotBefore());
  }

  @Test
  public void readCert() {
  }

}