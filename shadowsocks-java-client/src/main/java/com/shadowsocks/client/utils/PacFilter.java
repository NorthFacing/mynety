package com.shadowsocks.client.utils;

import com.shadowsocks.client.config.PacConfig;
import com.shadowsocks.client.config.ServerConfig;
import com.shadowsocks.common.utils.LocalCache;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;

public class PacFilter {

  private static final Logger logger = LoggerFactory.getLogger(PacFilter.class);

  /**
   * 判断是否拒绝连接
   *
   * @param domain 需要判断的域名
   * @return 拒绝连接 true，否则 false
   */
  public static boolean isDeny(String domain) {
    // 先看缓存中是否存在
    String denyDomain = LocalCache.get("deny-" + domain);
    if (StringUtils.isNotEmpty(denyDomain)) {
      return Boolean.valueOf(denyDomain);
    }
    boolean bl = regCheck(PacConfig.denyDomains, domain);
    LocalCache.set("deny-" + domain, Boolean.toString(bl), 300);
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
    String proxyDomain = LocalCache.get("proxy-" + domain);
    if (StringUtils.isNotEmpty(proxyDomain)) {
      return Boolean.valueOf(proxyDomain);
    }
    boolean bl = true;
    int strategy = ServerConfig.PROXY_STRATEGY;
    switch (strategy) {
      case 0: // 全局，则使用代理
        break;
      case 1: // PAC优先代理模式下，使用直连的域名来判断
        bl = !regCheck(PacConfig.directDomains, domain);
        break;
      case 2: // PAC优先直连模式下，使用代理的域名来判断
        bl = regCheck(PacConfig.proxyDomains, domain);
        break;
      default: // 默认开启全局
        break;
    }
    LocalCache.set("proxy-" + domain, Boolean.toString(bl));
    return bl;
  }

  /**
   * 正则验证：校验域名是否需要存在于配置的列表中
   *
   * @param confList 配置的域名集合
   * @param domain   需要校验的域名
   * @return domain正则匹配到confList中的元素就返回true，否则false
   */
  public static boolean regCheck(List<String> confList, String domain) {
    try {
      long start = System.currentTimeMillis();
      String result = confList.parallelStream()
          .filter(conf -> Pattern.matches("([a-z0-9]+[.])*" + conf, domain))
          .findAny()
          .orElse(null);
      long end = System.currentTimeMillis();
      logger.info("{} 域名校验 {} 毫秒：{}", LOG_MSG, (end - start), domain);
      return StringUtils.isNotEmpty(result) ? true : false;
    } catch (Exception e) {
      logger.error("域名验证出错：{}", e);
      return false;
    }
  }

}
