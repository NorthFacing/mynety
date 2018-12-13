package com.adolphor.mynety.server.lan;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
public class LanChannelContainers {

  /**
   * LAN客户端channel（所有请求共用此一个channel）
   */
  public static Channel lanChannel = null;

  /**
   * LAN客户端 requestId 对应的请求 channel，通过LAN channel返回数据中的requestId找到对应的channel并返回数据
   */
  private static final Map<String, Channel> requestChannels = new ConcurrentHashMap<>();

  public static Channel getChannelById(String requestId) {
    return requestChannels.get(requestId);
  }

  public static void addChannels(String requestId, Channel channel) {
    Channel put = requestChannels.put(requestId, channel);
    if (put == null) {
      logger.error("The channel associated with the given requestId has existed!");
    }
  }

}
