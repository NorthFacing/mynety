package com.adolphor.mynety.server.config;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * LAN PAC 自动切换
 * <p>
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
public class LanPacConfig {

  // PAC配置下，使用直连的域名
  public static final List<String> DIRECT_DOMAINS = new ArrayList<>();
  // 拒绝连接的域名
  public static final List<String> DENY_DOMAINS = new ArrayList<>();

}
