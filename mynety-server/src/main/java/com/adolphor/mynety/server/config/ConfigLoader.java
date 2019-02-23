package com.adolphor.mynety.server.config;

import com.adolphor.mynety.common.bean.BaseConfigLoader;
import com.adolphor.mynety.common.constants.LanStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 加载xml格式config
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
public class ConfigLoader {

  private static final String configFile = "server-config.yaml";

  public static void loadConfig() throws Exception {

    Map config = BaseConfigLoader.loadConfig(configFile);

    Map server = (Map) config.get("server");
    if (server != null) {
      Object socksPort = server.get("port");
      if (socksPort != null) {
        Config.PROXY_PORT = Integer.parseInt(socksPort.toString());
      }
      Object method = server.get("method");
      if (method != null) {
        Config.PROXY_METHOD = method.toString();
      }
      Object password = server.get("password");
      if (password != null) {
        Config.PROXY_PASSWORD = password.toString();
      }
    }

    Map lannet = (Map) config.get("lannet");
    if (lannet != null) {
      Object lanServerPort = lannet.get("lanPort");
      if (lanServerPort != null) {
        Config.LAN_SERVER_PORT = Integer.parseInt(lanServerPort.toString());
      }
      Object lanMethod = lannet.get("lanMethod");
      if (lanMethod != null) {
        Config.LAN_METHOD = lanMethod.toString();
      }
      Object lanPassword = lannet.get("lanPassword");
      if (lanPassword != null) {
        Config.LAN_PASSWORD = lanPassword.toString();
      }
      Object proxyStrategy = lannet.get("proxyStrategy");
      if (proxyStrategy != null) {
        Config.PROXY_STRATEGY = LanStrategy.getLanStrategyByVal(Integer.parseInt(proxyStrategy.toString()));
      }
      Object lanHostName = lannet.get("lanHostName");
      if (lanHostName != null) {
        Config.LAN_HOST_NAME = lanHostName.toString();
      }
    }
    logger.debug("Proxy server config loaded：Port={}, method={}, password={}", Config.PROXY_PORT, Config.PROXY_METHOD, Config.PROXY_PASSWORD);
    logger.debug("Proxy lannet config loaded：proxyStrategy={}, lanHostName={}, lanServerPort={}", Config.PROXY_STRATEGY, Config.LAN_HOST_NAME, Config.LAN_SERVER_PORT);
    logger.debug("Proxy lannet config loaded：lanMethod={}, lanPassword={}", Config.LAN_METHOD, Config.LAN_PASSWORD);
  }

}
