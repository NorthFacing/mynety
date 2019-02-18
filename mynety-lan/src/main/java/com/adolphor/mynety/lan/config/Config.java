package com.adolphor.mynety.lan.config;

import com.adolphor.mynety.common.bean.BaseConfig;
import com.adolphor.mynety.common.constants.Constants;

/**
 * lan configuration
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
public class Config extends BaseConfig {

  public static String METHOD = Constants.ENCRYPT_NONE;
  public static String PASSWORD = Constants.ENCRYPT_NONE;
  public static String LAN_SERVER_HOST = "127.0.0.1";
  public static int LAN_SERVER_PORT = 2087;

}
