/**
 * MIT License
 * <p>
 * Copyright (c) Bob.Zhu
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.shadowsocks.client.utils;

import com.shadowsocks.client.config.ClientConfig;
import com.shadowsocks.client.config.PacConfig;
import com.shadowsocks.common.utils.LocalCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

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
    String denyDomain = LocalCache.get("deny-" + domain);
    if (StringUtils.isNotEmpty(denyDomain)) {
      return Boolean.valueOf(denyDomain);
    }
    boolean bl = regCheck(PacConfig.denyDomains, domain);
    LocalCache.set("deny-" + domain, Boolean.toString(bl), 60 * 60 * 1000);
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
    int strategy = ClientConfig.PROXY_STRATEGY;
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
    LocalCache.set("proxy-" + domain, Boolean.toString(bl), 60 * 60 * 1000);
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
      logger.info("time of validate domain: {}s <= {}", (end - start), domain);
      return StringUtils.isNotEmpty(result) ? true : false;
    } catch (Exception e) {
      logger.error("error of validate domain: ", e);
      return false;
    }
  }

}
