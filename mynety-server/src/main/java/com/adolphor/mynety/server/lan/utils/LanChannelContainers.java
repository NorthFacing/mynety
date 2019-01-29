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
   * 连接LAN客户端的channel（所有请求共用此一个channel）
   */
  public static Channel lanChannel;

  /**
   * 每个channel对应的加解密对象
   */
  public static final Map<String, ICrypt> requestCryptsMap = new HashMap<>();

  /**
   * LAN客户端 requestId 对应的请求 channel，通过LAN channel返回数据中的requestId找到对应的channel并返回数据
   */
  private static final Map<String, Channel> requestChannels = new ConcurrentHashMap<>();

  /**
   * 需要评估是否这么实现，如果网络不稳定呢？
   * @return
   */
  public static Collection<Channel> getAllChannels(){
    return requestChannels.values();
  }

  public static Channel getChannelByRequestId(String requestId) {
    return requestChannels.get(requestId);
  }

  public static void addChannels(String requestId, Channel channel) {
    Channel put = requestChannels.put(requestId, channel);
    if (put != null) {
      logger.error("The channel associated with the given requestId has existed!");
    }
  }

}
