package com.adolphor.mynety.client.utils;

import com.adolphor.mynety.client.config.Server;
import com.adolphor.mynety.common.utils.LocalCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.telnet.TelnetClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.adolphor.mynety.client.config.Config.SERVERS;

/**
 * 网络工具
 * <p>
 * Internet util
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.2
 */
@Slf4j
public class NetUtils {

  /**
   * 获取ping时间最短的可用服务器
   * <p>
   * get a available and fastest server
   *
   * @return 可用服务器
   * @since v0.0.2
   */
  public static Server getBestServer() {
    if (SERVERS.size() == 0) {
      return null;
    }
    // 按照ping时间排序 sorted by time asc；不为空返回速度最快的一个服务器；为空随便返回一个，反正都不能用
    Server server = SERVERS.parallelStream()
        .filter(Server::isAvailable)
        .min(Comparator.comparingDouble(Server::getPingTime))
        .orElse(SERVERS.get(0));
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
            () -> SERVERS.forEach(server -> NetUtils.isConnected(server)),
            0,
            500,
            TimeUnit.SECONDS
        );
    // 查询 ping 所花费的时间，5 分钟执行一次
    Executors.newScheduledThreadPool(1)
        .scheduleWithFixedDelay(
            () -> SERVERS.forEach(server -> NetUtils.avgPingTime(server)),
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

  /**
   * 检测服务器是否可用
   * <p>
   * test server is available
   *
   * @param server 服务器实例
   */
  public static boolean isConnected(Server server) {
    boolean isAvailable = true;
    TelnetClient client = new TelnetClient();
    try {
      client.setDefaultTimeout(3000);
      client.connect(server.getHost(), server.getPort());
      client.disconnect();
    } catch (Exception e) {
      logger.warn("remote server: " + server.toString() + " connected failed!");
      isAvailable = false;
    }
    server.setAvailable(isAvailable);
    return isAvailable;
  }

  /**
   * 获取服务PING指令所消耗的时间
   * <p>
   * To get the average time of ping operation
   *
   * @param server 服务器实例
   * @return ping time
   */
  public static Double avgPingTime(Server server) {
    boolean isAvailable = server.isAvailable();
    if (!isAvailable) {
      return 999999.9;
    }

    ArrayList<Double> timeList = new ArrayList<>();
    try {
      Runtime r = Runtime.getRuntime();
      Process p = r.exec("ping " + server.getHost());
      BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String inputLine;
      int cnt = 0;
      while ((inputLine = in.readLine()) != null) {
        int startIndex = inputLine.indexOf("time=");
        int endIndex = inputLine.indexOf(" ms");
        if (startIndex < 0 || endIndex < 0) {
          continue;
        }
        String time = inputLine.substring(startIndex + "time=".length(), endIndex);
        timeList.add(Double.valueOf(time));
        if (++cnt > 8) {
          break;
        }
      }
      in.close();
      Thread.sleep(100);
    } catch (Exception e) {
      logger.warn("remote server: " + server.toString() + " telnet failed", e);
      return 999999.9;
    }

    Double avg = timeList.parallelStream().collect(Collectors.averagingDouble(p -> p));
    server.setPingTime(avg);
    return avg;
  }

  public static void main(String[] args) {
    String inputLine = "64 bytes from 107.175.23.142: icmp_seq=4 ttl=52 time=288.452 ms";
    int startIndex = inputLine.indexOf("time=");
    int endIndex = inputLine.indexOf(" ms");
    String time = inputLine.substring(startIndex + "time=".length(), endIndex);
    System.out.println(time);
  }

}
