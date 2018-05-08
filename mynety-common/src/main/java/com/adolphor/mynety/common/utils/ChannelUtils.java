package com.adolphor.mynety.common.utils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Map;

/**
 * channel 工具类
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public class ChannelUtils {

  /**
   * 调试工具，打印当前 channel pipeline 中的所有 handler，以及当前msg的数据类型
   *
   * @param channel
   */
  public static void loggerHandlers(Channel channel, Object msg) {
    logger.debug("=============================================start====================================================");
    logger.debug("msg type: {}", msg != null ? msg.getClass().getTypeName() : null);
    Iterator<Map.Entry<String, ChannelHandler>> iterator = channel.pipeline().iterator();
    iterator.forEachRemaining(handler -> {
      String key = handler.getKey();
      ChannelHandler value = handler.getValue();
      logger.debug(key + " => " + value);
    });
    logger.debug("===============================================end====================================================");

  }

}
