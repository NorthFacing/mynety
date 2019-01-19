package com.adolphor.mynety.common.utils;

import com.adolphor.mynety.common.bean.Address;
import io.netty.handler.codec.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.adolphor.mynety.common.constants.Constants.LOOPBACK_ADDRESS;
import static com.adolphor.mynety.common.constants.Constants.PATH_PATTERN;
import static com.adolphor.mynety.common.constants.Constants.PORT_443;
import static com.adolphor.mynety.common.constants.Constants.PORT_80;
import static com.adolphor.mynety.common.constants.Constants.SCHEME_HTTPS;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
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

  /**
   * 根据httpRequest对象，获取请求host和port
   * - http 请求的时候， httpRequest.uri() 获取的是完整请求路径
   * - https 请求的时候，httpRequest.uri() 获取的是 '域名:端口' 或者是 'IP:端口'
   *
   * @param httpRequest
   * @return
   * @since v0.0.5
   */
  public static Address getAddress(HttpRequest httpRequest) {
    String url = httpRequest.uri();
    if (StringUtils.isEmpty(url)) {
      new Address(LOOPBACK_ADDRESS, PORT_80);
    }
    Matcher matcher = PATH_PATTERN.matcher(url);
    if (!matcher.find()) {
      return new Address(LOOPBACK_ADDRESS, PORT_80);
    }
    String host = matcher.group();
    if (StringUtils.isEmpty(host)) {
      return new Address(LOOPBACK_ADDRESS, PORT_80);
    }
    if (host.contains(":")) {
      String[] ipPortArr = host.split(":");
      return new Address(ipPortArr[0], Integer.parseInt(ipPortArr[1]));
    }
    if (url.toLowerCase().startsWith(SCHEME_HTTPS)) {
      return new Address(host, PORT_443);
    }
    return new Address(host, PORT_80);

  }
}
