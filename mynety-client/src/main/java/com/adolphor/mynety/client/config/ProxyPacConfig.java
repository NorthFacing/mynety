package com.adolphor.mynety.client.config;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * PAC 自动切换
 * <p>
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.2
 */
@Slf4j
public class ProxyPacConfig {

  /**
   * PAC优先直连模式下，使用代理的域名
   */
  public static final List<String> PROXY_DOMAINS = new ArrayList<>();
  /**
   * PAC优先代理模式下，使用直连的域名
   */
  public static final List<String> DIRECT_DOMAINS = new ArrayList<>();
  /**
   * 拒绝连接的域名
   */
  public static final List<String> DENY_DOMAINS = new ArrayList<>();

}
