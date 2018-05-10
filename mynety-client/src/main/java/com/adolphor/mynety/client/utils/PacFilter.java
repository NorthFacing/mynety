package com.adolphor.mynety.client.utils;

import com.adolphor.mynety.client.config.ClientConfig;
import com.adolphor.mynety.client.config.ProxyPacConfig;
import com.adolphor.mynety.common.utils.DomainUtils;
import com.adolphor.mynety.common.utils.LocalCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import static com.adolphor.mynety.common.constants.CacheKey.PREFIX_PROXY_DENY;
import static com.adolphor.mynety.common.constants.CacheKey.PREFIX_PROXY_PROXY;

/**
 * 域名判断工具类
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.2
 */
@Slf4j
public class PacFilter {

  /**
   * 判断是否拒绝连接
   *
   * @param domain 需要判断的域名
   * @return 拒绝连接 true，否则 false
   */
  public static boolean isDeny(String domain) {
    // 先看缓存中是否存在
    String denyDomain = LocalCache.get(PREFIX_PROXY_DENY + domain);
    if (StringUtils.isNotEmpty(denyDomain)) {
      return Boolean.valueOf(denyDomain);
    }
    boolean bl = DomainUtils.regCheckForSubdomain(ProxyPacConfig.DENY_DOMAINS, domain);
    LocalCache.set(PREFIX_PROXY_DENY + domain, Boolean.toString(bl), 60 * 60 * 1000);
    return bl;
  }

  /**
   * 判断是否需要进行代理
   *
   * @param domain 需要判断的域名
   * @return 需要代理返回 true，否则 false
   */
  public static boolean isProxy(String domain) {
    // 先看缓存中是否存在
    String proxyDomain = LocalCache.get(PREFIX_PROXY_PROXY + domain);
    if (StringUtils.isNotEmpty(proxyDomain)) {
      return Boolean.valueOf(proxyDomain);
    }
    boolean bl = true;
    int strategy = ClientConfig.PROXY_STRATEGY;
    switch (strategy) {
      case 1:
        // PAC优先代理模式下，使用直连的域名来判断
        bl = !DomainUtils.regCheckForSubdomain(ProxyPacConfig.DIRECT_DOMAINS, domain);
        break;
      case 2:
        // PAC优先直连模式下，使用代理的域名来判断
        bl = DomainUtils.regCheckForSubdomain(ProxyPacConfig.PROXY_DOMAINS, domain);
        break;
      default:
        // 默认开启全局
        break;
    }
    LocalCache.set(PREFIX_PROXY_PROXY + domain, Boolean.toString(bl), 60 * 60 * 1000);
    return bl;
  }

}
