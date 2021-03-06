package com.adolphor.mynety.client.config;

import com.adolphor.mynety.common.bean.BaseConfigLoader;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static com.adolphor.mynety.client.config.ProxyPacConfig.DENY_DOMAINS;
import static com.adolphor.mynety.client.config.ProxyPacConfig.DIRECT_DOMAINS;
import static com.adolphor.mynety.client.config.ProxyPacConfig.PROXY_DOMAINS;
import static com.adolphor.mynety.common.constants.Constants.CONN_DENY;
import static com.adolphor.mynety.common.constants.Constants.CONN_DIRECT;
import static com.adolphor.mynety.common.constants.Constants.CONN_PROXY;

/**
 * load configuration
 * <p>
 * load config info
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
public class ConfigLoader {

  private final static String pacFileName = "pac.yaml";
  private final static String configFileName = "client-config.yaml";

  /**
   * <p>
   * load pac & client & server conf info
   *
   * @throws Exception
   */
  public static void loadConfig() throws Exception {

    loadPac(pacFileName);

    loadClientConf(configFileName);

  }

  private static void loadPac(String pacFile) throws Exception {
    try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(pacFile)) {
      if (is == null) {
        return;
      }

      Map pac = new Yaml().load(is);
      if (pac.get(CONN_PROXY) != null) {
        PROXY_DOMAINS.addAll((List) pac.get(CONN_PROXY));
      }
      if (pac.get(CONN_DIRECT) != null) {
        DIRECT_DOMAINS.addAll((List) pac.get(CONN_DIRECT));
      }
      if (pac.get(CONN_DENY) != null) {
        DENY_DOMAINS.addAll((List) pac.get(CONN_DENY));
      }
    }
  }


  private static void loadClientConf(String configFile) throws Exception {

    Map config = BaseConfigLoader.loadConfig(configFile);

    Object aPublic = config.get("public");
    if (aPublic != null) {
      Config.IS_PUBLIC = Boolean.valueOf(aPublic.toString());
    }
    Object socksLocalPort = config.get("socksLocalPort");
    if (socksLocalPort != null) {
      Config.SOCKS_PROXY_PORT = Integer.parseInt(socksLocalPort.toString());
    }
    Object httpLocalPort = config.get("httpLocalPort");
    if (httpLocalPort != null) {
      Config.HTTP_PROXY_PORT = Integer.parseInt(httpLocalPort.toString());
    }
    Object proxyStrategy = config.get("proxyStrategy");
    if (proxyStrategy != null) {
      Config.PROXY_STRATEGY = Integer.parseInt(proxyStrategy.toString());
    }

    if (config.get("servers") != null) {
      List<Map> servers = (List) config.get("servers");
      for (Map server : servers) {
        Server bean = new Server();
        Config.SERVERS.add(bean);

        Object remarks = server.get("remarks");
        if (remarks != null) {
          bean.setRemarks(remarks.toString());
        }
        Object host = server.get("host");
        if (host != null) {
          bean.setHost(host.toString());
        }
        Object port = server.get("port");
        if (port != null) {
          bean.setPort(Integer.parseInt(port.toString()));
        }
        Object method = server.get("method");
        if (method != null) {
          bean.setMethod(method.toString());
        }
        Object password = server.get("password");
        if (password != null) {
          bean.setPassword(password.toString());
        }
      }
    }
  }


}
