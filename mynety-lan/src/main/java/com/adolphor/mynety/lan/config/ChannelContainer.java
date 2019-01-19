package com.adolphor.mynety.lan.config;

import com.adolphor.mynety.common.encryption.ICrypt;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 目标地址连接相关容器
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
public class ChannelContainer {

  /**
   * 各个requestId的缓存列表
   */
  private static final Map<String, ReqChannel> requestChannelsMap = new HashMap<>();

  /**
   * 初始化连接目标地址的channel对应的相关参数
   *
   * @param requestId
   * @param crypt
   */
  public static void initChannelConfig(String requestId, ICrypt crypt) {
    requestChannelsMap.put(requestId, new ReqChannel(requestId, crypt));
    logger.debug("【ChannelContainer】initChannelConfig 新增之后缓存的 channel 个数：{}", requestChannelsMap.size());
  }

  public static AtomicReference<Channel> getOutRelayChannelRef(String requestId) {
    ReqChannel reqChannel = requestChannelsMap.get(requestId);
    if (reqChannel != null) {
      return reqChannel.getOutRelayChannelRef();
    }
    return null;
  }

  public static AtomicReference<ByteBuf> getTempMsgRef(String requestId) {
    ReqChannel reqChannel = requestChannelsMap.get(requestId);
    if (reqChannel != null) {
      return reqChannel.getTempMsgRef();
    }
    return null;
  }

  public static ICrypt getCrypt(String requestId) {
    ReqChannel reqChannel = requestChannelsMap.get(requestId);
    if (reqChannel != null) {
      return reqChannel.getCrypt();
    }
    return null;
  }

  public static void removeReqChannel(String requestId) {
    requestChannelsMap.remove(requestId);
    logger.debug("【ChannelContainer】removeReqChannel 移除之后缓存的 channel 个数：{}", requestChannelsMap.size());
  }

  /**
   * 当 ss-server 告诉 lan 断开的时候，移除映射关系
   *
   * @param requestId
   * @return
   */
  public static void removeOutRelayChannel(String requestId) {
    requestChannelsMap.remove(requestId);
  }

}
