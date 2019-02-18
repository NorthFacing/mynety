package com.adolphor.mynety.server.lan.utils;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

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
   * val: lanClient
   */
  public static final Map<String, LanClient> lanMaps = new HashMap<>();
  /**
   * channel, connected to lan client
   */
  public static Channel clientMainChannel;

}
