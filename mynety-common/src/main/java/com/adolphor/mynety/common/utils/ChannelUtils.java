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
 * netty 的 socket 工具类
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
public class ChannelUtils {

  /**
   * 关闭通道：
   * 使用listener方式，保证数据传输完毕之后才关闭channel
   *
   * @param ch
   */
  public static void closeOnFlush(Channel ch) {
    if (ch != null) {
      ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
  }

  /**
   * 调试工具，打印当前 channel pipeline 中的所有 handler，以及当前msg的数据类型
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
    logger.debug("===============================================end====================================================");
  }

}
