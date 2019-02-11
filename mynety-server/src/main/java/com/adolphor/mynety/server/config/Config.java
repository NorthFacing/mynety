package com.adolphor.mynety.server.config;

import com.adolphor.mynety.common.constants.LanStrategy;

import static com.adolphor.mynety.common.constants.Constants.ENCRYPT_NONE;

/**
 * 服务端配置
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
public class Config {

  public static int PROXY_PORT = 2086;
  public static String PROXY_METHOD = "aes-256-cfb";
  public static String PROXY_PASSWORD = "123456˚";

  public static int LAN_SERVER_PORT = 2087;
  public static LanStrategy LAN_STRATEGY = LanStrategy.ALL;
  public static String LAN_HOST_NAME = "mynetylan.adolphor.com";
  public static String LAN_METHOD = ENCRYPT_NONE;
  public static String LAN_PASSWORD = ENCRYPT_NONE;

}
