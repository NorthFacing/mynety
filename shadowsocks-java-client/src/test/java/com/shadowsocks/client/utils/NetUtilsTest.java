package com.shadowsocks.client.utils;

import com.shadowsocks.client.config.Server;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NetUtilsTest {

  private static Server server = new Server();

  @Before
  public void newServer(){
    server.setHost("127.0.0.1");
    server.setPort(1081);
  }

  @Test
  public void isConnected() {
    boolean connected = NetUtils.isConnected(server);
    Assert.assertFalse(connected);
  }

  @Test
  public void avgPingTime() {
    Double pingTime = NetUtils.avgPingTime(server);
    Assert.assertNotNull(pingTime);
  }
}