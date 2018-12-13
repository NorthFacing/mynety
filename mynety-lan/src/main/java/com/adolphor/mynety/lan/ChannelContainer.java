package com.adolphor.mynety.lan;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
public class ChannelContainer {

  /**
   * 各个requestId对应的channel（也就是各个目标地址对应的channel）
   */
  private static final Map<String, AtomicReference<Channel>> remoteChannels = new HashMap<>();

  /**
   * requestId 请求列表
   */
  private static final List<String> remoteRequests = new ArrayList<>();

  private static final Map<String, List<ByteBuf>> requestTempListMap = new HashMap<>();

  /**
   * 获取requestId对应的channel（如果不存在，那么就维护一个新的对应关系并返回）
   *
   * @param requestId
   * @return
   */
  public static synchronized AtomicReference<Channel> getRemoteChannelRef(String requestId) {
    AtomicReference<Channel> remoteChannelRef = remoteChannels.get(requestId);
    if (remoteChannelRef != null) {
      return remoteChannelRef;
    } else {
      AtomicReference<Channel> newRemoteRef = new AtomicReference<>();
      remoteChannels.put(requestId, newRemoteRef);
      return newRemoteRef;
    }
  }

  public static synchronized AtomicReference<Channel> removeRemoteChannelRef(String requestId) {
    return remoteChannels.remove(requestId);
  }

  /**
   * 对于给定的 requestId 是否已经存在
   *
   * @param requestId
   * @return
   */
  public static synchronized boolean isRequested(String requestId) {
    Boolean exist = remoteRequests.contains(requestId);
    if (exist) {
      return true;
    } else {
      remoteRequests.add(requestId);
      return false;
    }
  }

  public static synchronized List<ByteBuf> getRequestTempList(String requestId) {
    List<ByteBuf> requestTempList = requestTempListMap.get(requestId);
    if (requestTempList == null) {
      requestTempList = new ArrayList<>();
      requestTempListMap.put(requestId, requestTempList);
    }
    return requestTempList;
  }

  /**
   * 缓存的request请求是否为空（只有缓存的请求消费完之后才直接消费后续请求，否则就放在缓存列表中）
   *
   * @param requestId
   * @return
   */
  public static boolean isTempEmpty(String requestId) {
    List<ByteBuf> list = requestTempListMap.get(requestId);

    if (list == null || list.size() == 0) {
      return true;
    } else {
      return false;
    }
  }

}
