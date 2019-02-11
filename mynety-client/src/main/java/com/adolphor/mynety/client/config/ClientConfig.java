package com.adolphor.mynety.client.config;

import com.adolphor.mynety.client.utils.cert.CertConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端使用的服务器配置
 * <p>
 * server configs for proxy client
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
public class ClientConfig {

  public static boolean IS_PUBLIC = true;
  public static int SOCKS_PROXY_PORT = 1086;
  public static int HTTP_PROXY_PORT = 1087;
  public static boolean HTTP_2_SOCKS5 = true;
  public static boolean HTTP_MITM = false;

  public static String CA_PASSWORD = "mynety-ca-password";
  public static String CA_KEYSTORE_FILE = "mynety-root-ca.jks";

  public static int PROXY_STRATEGY = 0;

  public static final CertConfig HTTPS_CERT_CONFIG = new CertConfig();

  public static final List<Server> SERVERS = new ArrayList<>();

}