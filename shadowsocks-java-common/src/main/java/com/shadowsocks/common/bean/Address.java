package com.shadowsocks.common.bean;

import lombok.Data;

/**
 * 请求路径解析
 *
 * @Email 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Data
public class Address {

  private String scheme;
  private String host;
  private int port;
  private String path;

  public Address(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public Address(String scheme, String host, int port, String path) {
    this.scheme = scheme;
    this.host = host;
    this.port = port;
    this.path = path;
  }

}
