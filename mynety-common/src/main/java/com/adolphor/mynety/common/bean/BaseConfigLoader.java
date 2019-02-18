package com.adolphor.mynety.common.bean;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.6
 */
public class BaseConfigLoader {

  public static Map loadConfig(String configFile) throws Exception {

    try (InputStream is = BaseConfigLoader.class.getClassLoader().getResourceAsStream(configFile)) {
      Map<String, String> config = new Yaml().load(is);
      Object nativeNio = config.get("nativeNio");
      if (nativeNio != null) {
        BaseConfig.nativeNio = Boolean.valueOf(nativeNio.toString());
      }
      return config;
    }
  }
}
