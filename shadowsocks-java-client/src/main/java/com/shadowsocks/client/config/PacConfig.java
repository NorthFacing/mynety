package com.shadowsocks.client.config;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * PAC 自动切换
 * <p>
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.2
 */
@Slf4j
public class PacConfig {

  // PAC优先直连模式下，使用代理的域名
  public static List<String> proxyDomains = new ArrayList<>();
  // PAC优先代理模式下，使用直连的域名
  public static List<String> directDomains = new ArrayList<>();
  // 拒绝连接的域名
  public static List<String> denyDomains = new ArrayList<>();

}
