package com.adolphor.mynety.client.utils.cert;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class CertPool {

  private static Map<Integer, Map<String, X509Certificate>> certCache = new WeakHashMap<>();

  public static X509Certificate getCert(Integer port, String host, HttpsCertConfig serverConfig)
      throws Exception {
    X509Certificate cert = null;
    if (host != null) {
      Map<String, X509Certificate> portCertCache = certCache.get(port);
      if (portCertCache == null) {
        portCertCache = new HashMap<>();
        certCache.put(port, portCertCache);
      }
      String key = host.trim().toLowerCase();
      if (portCertCache.containsKey(key)) {
        return portCertCache.get(key);
      } else {
        cert = CertUtils.genCert(serverConfig.getIssuer(), serverConfig.getCaPrivateKey(),
            serverConfig.getCaNotBefore(), serverConfig.getCaNotAfter(),
            serverConfig.getPublicKey(), key);
        portCertCache.put(key, cert);
      }
    }
    return cert;
  }

  public static void clear() {
    certCache.clear();
  }
}
