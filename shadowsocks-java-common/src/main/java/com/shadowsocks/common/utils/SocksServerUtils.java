package com.shadowsocks.common.utils;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * netty 的 socket 工具类
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Slf4j
public class SocksServerUtils {

  /**
   * 关闭之前刷新一次数据
   *
   * @param ch
   */
  public static void flushOnClose(Channel ch) {
    if (ch != null && ch.isActive()) {
      ch.flush();
      ch.close();
    }
  }

}
