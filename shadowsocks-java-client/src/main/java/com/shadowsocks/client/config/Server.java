package com.shadowsocks.client.config;

import com.shadowsocks.common.encryption.ICrypt;

/**
 * 服务器信息
 * <p>
 * server info domain
 *
 * @since v0.0.1
 */
public class Server {

  private String remarks; // 服务器备注名称
  private String host;
  private int port;
  private String method;
  private String password;
  private ICrypt crypt;
  private boolean available = true; // 是否可用
  private Double pingTime;

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public ICrypt getCrypt() {
    return crypt;
  }

  public void setCrypt(ICrypt crypt) {
    this.crypt = crypt;
  }

  public boolean isAvailable() {
    return available;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }

  public Double getPingTime() {
    return pingTime;
  }

  public void setPingTime(Double pingTime) {
    this.pingTime = pingTime;
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(this.host).append(":").append(this.port).append(" => ")
        .append(this.method).append(" / ").append(this.password)
        .append(" => ").append(this.available);
    return sb.toString();
  }
}
