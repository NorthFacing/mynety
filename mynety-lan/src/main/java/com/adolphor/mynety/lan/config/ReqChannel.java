package com.adolphor.mynety.lan.config;

import com.adolphor.mynety.common.encryption.ICrypt;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.Data;

import java.util.concurrent.atomic.AtomicReference;

@Data
public class ReqChannel {

  /**
   * 请求ID
   */
  private final String requestId;
  /**
   * 加解密对象，每个channel要维护自己的加解密对象
   */
  private final ICrypt crypt;
  /**
   * 缓存信息
   */
  private final AtomicReference<ByteBuf> tempMsgRef = new AtomicReference<>();
  /**
   * 目标地址连接channel
   */
  private final AtomicReference<Channel> outRelayChannelRef = new AtomicReference<>();

  public ReqChannel(String requestId,ICrypt crypt) {
    this.crypt = crypt;
    this.requestId = requestId;
    // 初始化缓存对象容器
    tempMsgRef.set(Unpooled.directBuffer());
  }
}
