package com.adolphor.mynety.client.config;

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
 * 加载配置信息
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
   * 分别加载 pac 和 client 和 server 配置信息
   * <p>
   * load pac & client & server conf info
   *
   * @throws Exception
   */
  public static void loadConfig() throws Exception {
    // PAC 模式配置
    loadPac(pacFileName);

    // 服务器资源配置
    // 先加载测试环境配置，如果为空再去找正式环境配置（为了调试开发方便）
    loadClientConf("dev-" + configFileName);
    if (ClientConfig.getAvailableServer() == null) {
      loadClientConf(configFileName);
    }
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

    try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(configFile)) {
      if (is == null) {
        // dev文件为空直接返回接着找pro文件；如果pro文件也为空，就说明缺少配置文件
        if (configFileName.equals(configFile)) {
          throw new NullPointerException("缺少 client-config 配置文件！");
        }
        return;
      }

      Map config = new Yaml().load(is);
      Object aPublic = config.get("public");
      if (aPublic != null) {
        ClientConfig.IS_PUBLIC = Boolean.valueOf(aPublic.toString());
      }
      Object socksLocalPort = config.get("socksLocalPort");
      if (socksLocalPort != null) {
        ClientConfig.SOCKS_PROXY_PORT = Integer.valueOf(socksLocalPort.toString());
      }
      Object httpLocalPort = config.get("httpLocalPort");
      if (httpLocalPort != null) {
        ClientConfig.HTTP_PROXY_PORT = Integer.valueOf(httpLocalPort.toString());
      }
      Object http2socks5 = config.get("http2socks5");
      if (http2socks5 != null) {
        ClientConfig.HTTP_2_SOCKS5 = Boolean.valueOf(http2socks5.toString());
      }
      Object handleSsl = config.get("handleSsl");
      if (http2socks5 != null) {
        ClientConfig.HANDLE_SSL = Boolean.valueOf(handleSsl.toString());
      }
      Object proxyStrategy = config.get("proxyStrategy");
      if (proxyStrategy != null) {
        ClientConfig.PROXY_STRATEGY = Integer.valueOf(proxyStrategy.toString());
      }

      if (config.get("servers") != null) {
        List<Map> servers = (List) config.get("servers");
        for (Map server : servers) {
          Server bean = new Server();
          ClientConfig.addServer(bean);

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
            bean.setPort(Integer.valueOf(port.toString()));
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


}
