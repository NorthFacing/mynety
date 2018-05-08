package com.adolphor.mynety.client.config;

import com.adolphor.mynety.common.encryption.ICrypt;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 服务器信息
 * <p>
 * server info domain
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Slf4j
@Data
public class Server {

  private String remarks; // 服务器备注名称
  private String host;
  private int port;
  private String method;
  private String password;
  private ICrypt crypt;
  private boolean available = true; // 是否可用
  private Double pingTime = 0d;

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(this.host).append(":").append(this.port).append(" => ")
        .append(this.method).append(" / ").append(this.password)
        .append(" => ").append(this.available);
    return sb.toString();
  }

}
