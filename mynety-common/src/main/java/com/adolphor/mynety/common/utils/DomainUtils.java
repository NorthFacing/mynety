package com.adolphor.mynety.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @refactor v0.0.5
 * @since v0.0.1
 */
@Slf4j
public class DomainUtils {

  /**
   * socks代理域名规则验证
   *
   * @param confList 配置的域名集合
   * @param domain   需要校验的域名
   */
  public static boolean regCheckForSubdomain(List<String> confList, String domain) {
    final String prefix = "([a-z0-9]+[.])*";
    return regexCheck(confList, prefix, domain);
  }

  /**
   * 内网映射列表域名验证
   *
   * @param confList 配置的域名集合
   * @param domain   需要校验的域名
   */
  public static boolean regCheckForLanDomain(List<String> confList, String domain) {
    return confList.contains(domain);
  }

  /**
   * 正则验证：校验域名是否需要存在于配置的列表中
   *
   * @param confList 配置的域名集合
   * @param domain   需要校验的域名
   * @return domain正则匹配到confList中的元素就返回true，否则false
   */
  public static boolean regexCheck(List<String> confList, final String prefix, String domain) {
    try {
      long start = System.currentTimeMillis();
      String result = confList.parallelStream()
          .filter(conf -> Pattern.matches(prefix + conf, domain))
          .findAny()
          .orElse(null);
      long end = System.currentTimeMillis();
      logger.info("time of validate domain: {}ms <= {}", (end - start), domain);
      return StringUtils.isNotEmpty(result) ? true : false;
    } catch (Exception e) {
      logger.error("error of validate domain: ", e);
      return false;
    }
  }

}
