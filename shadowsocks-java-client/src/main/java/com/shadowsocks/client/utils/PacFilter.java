package com.shadowsocks.client.utils;

import com.shadowsocks.client.config.PacConfig;
import com.shadowsocks.client.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;

public class PacFilter {

  private static final Logger logger = LoggerFactory.getLogger(PacFilter.class);

  /**
   * 判断是否拒绝连接
   *
   * @param domain 需要判断的域名
   * @return 拒绝连接 true，否则 false
   */
  public static boolean isDeny(String domain) {
    return regCheck(PacConfig.denyDomains, domain);
  }

  /**
   * 判断是否需要进行代理
   *
   * @param domain 需要判断的域名
   * @return 需要代理返回 true，否则 false
   */
  public static boolean isProxy(String domain) {
    // 先看缓存中是否存在
    if (PacConfig.cachedStrategy.containsKey(domain)) {
      Boolean bl = PacConfig.cachedStrategy.get(domain);
      logger.debug("是否使用代理命中缓存信息：{} => {}", domain, bl);
      return bl;
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
    PacConfig.cachedStrategy.put(domain, bl);
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
      long match = confList.parallelStream()
          .filter(conf -> Pattern.matches("^(\\w?.?)+" + conf, domain))
          .count();
      return match > 0 ? true : false;
    } catch (Exception e) {
      logger.error("域名验证出错：{}", e);
      return false;
    }
  }

}
