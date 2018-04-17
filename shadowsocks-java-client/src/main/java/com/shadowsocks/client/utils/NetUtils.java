/**
 * MIT License
 * <p>
 * Copyright (c) 2018 0haizhu0@gmail.com
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

import com.shadowsocks.client.config.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.telnet.TelnetClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 网络工具
 * <p>
 * Internet util
 *
 * @author 0haizhu0@gmail.com
 * @since v0.0.2
 */
@Slf4j
public class NetUtils {

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
      log.warn("remote server: " + server.toString() + " connected failed");
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
      log.warn("remote server: " + server.toString() + " telnet failed");
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
