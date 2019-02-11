package com.adolphor.mynety.lan.config;

import com.adolphor.mynety.common.constants.Constants;

/**
 * 服务端配置
 * TODO 需要增加连接权限验证
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
public class Config {

  public static String METHOD = Constants.ENCRYPT_NONE;
  public static String PASSWORD = Constants.ENCRYPT_NONE;
  public static String LAN_SERVER_HOST = "127.0.0.1";
  public static int LAN_SERVER_PORT = 2087;

}
