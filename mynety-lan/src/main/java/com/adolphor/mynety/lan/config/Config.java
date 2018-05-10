package com.adolphor.mynety.lan.config;

/**
 * 服务端配置
 * TODO 需要增加连接权限验证
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.5
 */
public class Config {

  public static String METHOD = "aes-256-cfb";
  public static String PASSWORD = "123456";

  /**
   * LAN 服务器域名地址
   */
  public static String LAN_SERVER_HOST = "127.0.0.1";
  /**
   * LAN 服务器端口号
   */
  public static int LAN_SERVER_PORT = 2187;

}
