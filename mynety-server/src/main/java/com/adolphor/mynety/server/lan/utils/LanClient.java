package com.adolphor.mynety.server.lan.utils;

import com.adolphor.mynety.common.encryption.ICrypt;
import io.netty.channel.Channel;
import lombok.Data;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.6
 */
@Data
public class LanClient {

  /**
   * crypt instance
   */
  private ICrypt crypt;
  /**
   * inRelayChannel from socks server
   */
  private Channel inRelayChannel;

  public LanClient(Channel inRelayChannel, ICrypt crypt) {
    this.crypt = crypt;
    this.inRelayChannel = inRelayChannel;
  }

}
