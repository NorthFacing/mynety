package com.adolphor.mynety.server.lan.utils;

import com.adolphor.mynety.common.encryption.ICrypt;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.HashMap;
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
   * container to store crypt instance:
   * key: requestId
   * val: crypt instance
   */
  public static final Map<String, ICrypt> requestCryptsMap = new HashMap<>();
  /**
   * container to store crypt instance:
   * key: requestId
   * val: inRelayChannel from socks server
   */
  private static final Map<String, Channel> requestChannels = new ConcurrentHashMap<>();
  /**
   * channel, connected to lan client
   */
  public static Channel lanChannel;

  /**
   * get all reRelayChannels
   */
  public static Collection<Channel> getAllChannels() {
    return requestChannels.values();
  }

  public static Channel getChannelByRequestId(String requestId) {
    return requestChannels.get(requestId);
  }

  public static void addChannels(String requestId, Channel channel) {
    Channel put = requestChannels.put(requestId, channel);
    if (put != null) {
      logger.warn("The channel associated with the given requestId has existed: {}", requestId);
    }
  }

}
