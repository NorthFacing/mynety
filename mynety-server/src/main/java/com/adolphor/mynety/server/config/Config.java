package com.adolphor.mynety.server.config;

/**
 * 服务端配置
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.1
 */
public class Config {


  public static int PROXY_PORT = 2086;
  public static String METHOD;
  public static String PASSWORD;

  /**
   * 内网映射转发策略：
   * -1: 关闭
   * 0： 全部转发至内网 (默认)
   * 1： 指定域名转发至内网
   */
  public static int LAN_STRATEGY = 0;
  public static int LAN_SERVER_PORT = 2187;
  /**
   * 内网本地服务域名
   */
  public static String LAN_HOST_NAME = "mynetylan.adolphor.com";

}
