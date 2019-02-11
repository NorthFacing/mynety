package com.adolphor.mynety.client.utils.cert;

import org.apache.commons.lang3.StringUtils;

import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
public class CertPool {

  private static Map<String, X509Certificate> certCache = new WeakHashMap<>();

  public static X509Certificate getCert(String host, CertConfig serverConfig) throws Exception {
    X509Certificate cert;
    if (StringUtils.isEmpty(host)) {
      return null;
    }
    cert = certCache.get(host);
    if (cert == null) {
      cert = CertUtils.genMitmCert(serverConfig.getIssuer(),
          serverConfig.getCaPriKey(),
          serverConfig.getNotBefore(),
          serverConfig.getNotAfter(),
          serverConfig.getMitmPubKey(),
          host);
      certCache.put(host, cert);
    }
    return cert;
  }

  public static void clear() {
    certCache.clear();
  }
}
