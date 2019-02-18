package com.adolphor.mynety.lan.config;

import com.adolphor.mynety.common.bean.BaseConfigLoader;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 加载xml格式config
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
public class ConfigLoader {

  private static final String configFile = "lan-config.yaml";

  public static void loadConfig() throws Exception {

    Map config = BaseConfigLoader.loadConfig(configFile);

    Config.METHOD = config.get("method").toString();
    Config.PASSWORD = config.get("password").toString();
    Config.LAN_SERVER_HOST = config.get("serverHost").toString();
    Config.LAN_SERVER_PORT = Integer.parseInt(config.get("serverPort").toString());

    logger.debug("Lan client config loads success：serverHost={}, serverPort={}", Config.LAN_SERVER_HOST, Config.LAN_SERVER_PORT);
  }

}
