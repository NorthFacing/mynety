package com.adolphor.mynety.server.config;

import com.adolphor.mynety.common.constants.LanStrategy;

/**
 * 服务端配置
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
public class Config {

  /**
   * ss服务器断开
   */
  public static int PROXY_PORT = 2086;
  /**
   * ss加密方式
   */
  public static String PROXY_METHOD;
  /**
   * ss加密密码
   */
  public static String PROXY_PASSWORD;

  /**
   * 内网穿透映射/转发服务端口
   */
  public static int LAN_SERVER_PORT = 2087;
  /**
   * 内网穿透映射/转发策略：
   * -1： 关闭
   * 0：全部转发至内网 (默认)
   * 1：指定网址转发至内网（后续看情况再实现及优化）
   */
  public static LanStrategy LAN_STRATEGY = LanStrategy.ALL;
  /**
   * 内网穿透映射域名，不指定的话默认"mynetylan.adolphor.com"，此域名下的所有请求，会转发到运行LAN服务的 "127.0.0.1"
   */
  public static String LAN_HOST_NAME = "mynetylan.adolphor.com";

}
