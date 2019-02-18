package com.adolphor.mynety.common.bean;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.6
 */
public class BaseConfig {

  /**
   * ignore operation system, force to using NIO class of netty.
   * such as raspberry is linux, but cannot to use epoll.
   */
  public static boolean nativeNio = false;

}
