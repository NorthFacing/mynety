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
package com.shadowsocks.client.config;

import com.shadowsocks.client.utils.NetUtils;
import com.shadowsocks.common.utils.LocalCache;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 客户端使用的服务器配置
 * <p>
 * server configs for proxy client
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Slf4j
public class ClientConfig {

  /**
   * 是否开放本机代理给其他设备使用
   */
  public static boolean IS_PUBLIC = true;

  public static int SOCKS_PROXY_PORT = 1086;
  public static int HTTP_PROXY_PORT = 1087;

  // 默认为true，所有 HTTP 请求都进行socks5代理转发
  public static boolean HTTP_2_SOCKS5 = true;

  /**
   * 代理模式可选项：
   * 0：全局      Globle
   * 1：优先代理  prefer proxy
   * 2：优先本地  prefer direct
   */
  public static int PROXY_STRATEGY = 0;

  public static final List<Server> servers = new ArrayList<>();

  public static void addServer(Server server) {
    servers.add(server);
  }

  /**
   * 获取ping时间最短的可用服务器
   * <p>
   * get a available and fastest server
   *
   * @return 可用服务器
   * @since v0.0.2
   */
  public static Server getAvailableServer() {
    if (servers.size() == 0) {
      return null;
    }

    // 按照ping时间排序 sorted by time asc
    Server server = servers.parallelStream()
        .filter(Server::isAvailable)
        .min(Comparator.comparingDouble(Server::getPingTime))
        .orElse(servers.get(0)); // 不为空返回速度最快的一个服务器；为空随便返回一个，反正都不能用
    return server;
  }

  /**
   * 检测服务器列表中服务器可用性
   * <p>
   * check availability of a server of the servers list
   *
   * @since v0.0.2
   */
  public static void checkServers() {
    // 验证服务器是否可用，30 秒执行一次
    Executors.newScheduledThreadPool(1)
        .scheduleWithFixedDelay(
            () -> servers.forEach(server -> NetUtils.isConnected(server)),
            0,
            500,
            TimeUnit.SECONDS
        );
    // 查询 ping 所花费的时间，5 分钟执行一次
    Executors.newScheduledThreadPool(1)
        .scheduleWithFixedDelay(
            () -> servers.forEach(server -> NetUtils.avgPingTime(server)),
            0,
            300,
            TimeUnit.SECONDS
        );
    // 检测域名domain缓存时间，剔除最近不再使用的数据节省本地缓存空间
    Executors.newScheduledThreadPool(1)
        .scheduleWithFixedDelay(
            () -> LocalCache.validateForGC(10000),
            0,
            600,
            TimeUnit.SECONDS
        );

  }

}