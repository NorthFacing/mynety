package com.adolphor.mynety.server.utils;

import com.adolphor.mynety.common.utils.DomainUtils;
import com.adolphor.mynety.common.utils.LocalCache;
import com.adolphor.mynety.server.config.Config;
import com.adolphor.mynety.server.config.LanPacConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import static com.adolphor.mynety.common.constants.CacheKey.PREFIX_LAN_DENY;
import static com.adolphor.mynety.common.constants.CacheKey.PREFIX_LAN_PROXY;

/**
 * LAN转发域名判断工具类
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.5
 */
@Slf4j
public class LanPacFilter {

  /**
   * 判断是否拒绝连接
   *
   * @param domain 需要判断的域名
   * @return 拒绝连接 true，否则 false
   */
  public static boolean isDeny(String domain) {
    // 先看缓存中是否存在
    String denyDomain = LocalCache.get(PREFIX_LAN_DENY + domain);
    if (StringUtils.isNotEmpty(denyDomain)) {
      return Boolean.valueOf(denyDomain);
    }
    boolean bl = DomainUtils.regCheckForSubdomain(LanPacConfig.DENY_DOMAINS, domain);
    LocalCache.set(PREFIX_LAN_DENY + domain, Boolean.toString(bl), 60 * 60 * 1000);
    return bl;
  }

  /**
   * 判断是否需要转发到LAN
   *
   * @param domain 需要判断的域名
   * @return 需要转发 返回 true，否则 false
   */
  public static boolean isProxy(String domain) {
    // 先看缓存中是否存在
    String proxyDomain = LocalCache.get(PREFIX_LAN_PROXY + domain);
    if (StringUtils.isNotEmpty(proxyDomain)) {
      return Boolean.valueOf(proxyDomain);
    }
    boolean bl = true;
    int strategy = Config.LAN_STRATEGY;
    switch (strategy) {
      case -1:
        // 不开启LAN内网穿透
        bl = false;
        break;
      case 1:
        // 指定域名转发至内网
        bl = !DomainUtils.regCheckForSubdomain(LanPacConfig.DIRECT_DOMAINS, domain);
        break;
      default:
        // 除此之外，不用判断全部转发至内网
        break;
    }
    LocalCache.set(PREFIX_LAN_PROXY + domain, Boolean.toString(bl), 60 * 60 * 1000);
    return bl;
  }

}
