package com.shadowsocks.client.config;

import com.shadowsocks.client.utils.NetUtils;
import com.shadowsocks.common.encryption.ICrypt;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 服务器配置
 * <p>
 * servers config
 *
 * @since v0.0.1
 */
public class ServerConfig {

  private static final Logger logger = LoggerFactory.getLogger(ServerConfig.class);

  public static boolean PUBLIC = true;

  public static int LOCAL_PORT = 1086;

  /**
   * 代理模式可选项：
   * 0：全局      Globle
   * 1：优先代理  prefer proxy
   * 2：优先本地  prefer direct
   */
  public static int PROXY_STRATEGY = 0;

  public static final List<Server> servers = new ArrayList<>();

  public static final AttributeKey<ICrypt> CRYPT_KEY = AttributeKey.valueOf("crypt");
  public static final AttributeKey<Boolean> IS_PROXY = AttributeKey.valueOf("isProxy");
  public static final AttributeKey<String> DST_ADDR = AttributeKey.valueOf("dstAddr");
  public static final AttributeKey<Channel> CLIENT_CHANNEL = AttributeKey.valueOf("clientChannel");
  public static final AttributeKey<Channel> REMOTE_CHANNEL = AttributeKey.valueOf("remoteChannel");

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
        .get();
    return server;
  }

  /**
   * 检测服务器列表中服务器可用性
   * <p>
   * check availability of a server of the servers list
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
  }

}