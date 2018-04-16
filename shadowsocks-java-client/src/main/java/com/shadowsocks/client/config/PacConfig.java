package com.shadowsocks.client.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PAC 自动切换
 * <p>
 * v0.0.2
 */
public class PacConfig {

  // PAC优先直连模式下，使用代理的域名
  public static List<String> proxyDomains = new ArrayList<>();
  // PAC优先代理模式下，使用直连的域名
  public static List<String> directDomains = new ArrayList<>();
  // 拒绝连接的域名
  public static List<String> denyDomains = new ArrayList<>();

  // 缓存的配置策略
  public static final Map<String, Boolean> cachedProxyStrategy = new HashMap<>();
  // 缓存的拒绝连接地址
  public static final Map<String, Boolean> cachedDenyDomains = new HashMap<>();

}
