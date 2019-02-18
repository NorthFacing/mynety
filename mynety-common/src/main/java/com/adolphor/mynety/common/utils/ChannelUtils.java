package com.adolphor.mynety.common.utils;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Map;

import static org.apache.commons.lang3.ClassUtils.getName;

/**
 * socks utils
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
public class ChannelUtils {

  /**
   * close the channel after all data has been transmitted
   *
   * @param ch
   */
  public static void closeOnFlush(Channel ch) {
    if (ch != null) {
      ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
  }

  /**
   * debug method: log all handler in the pipeline
   *
   * @param channel
   */
  public static void loggerHandlers(Object clazz, String method, Channel channel, Object msg) {
    logger.debug("=============================================start====================================================");
    logger.debug("in class: {} => {}", getName(clazz), method);
    logger.debug("msg type: {}", msg != null ? msg.getClass().getTypeName() : null);
    Iterator<Map.Entry<String, ChannelHandler>> iterator = channel.pipeline().iterator();
    iterator.forEachRemaining(handler -> {
      String key = handler.getKey();
      ChannelHandler value = handler.getValue();
      logger.debug("[ {} ] {} => {}", channel.id(), key, value);
    });
    logger.debug("=============================================end======================================================");
  }

}
