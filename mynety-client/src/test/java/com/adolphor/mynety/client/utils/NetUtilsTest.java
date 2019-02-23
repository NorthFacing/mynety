package com.adolphor.mynety.client.utils;

import com.adolphor.mynety.client.config.Config;
import com.adolphor.mynety.client.config.Server;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NetUtilsTest {

  private static Server server = new Server();

  @Before
  public void newServer() {
    server.setHost("127.0.0.1");
    server.setPort(1081);
  }

  @Test
  public void getBestServer() {
    double time = 100d;
    for (int i = 0; i < 5; i++) {
      Server server = new Server();
      server.setPingTime(time + i);
      Config.SERVERS.add(server);
    }

    Server server = NetUtils.getBestServer();
    Assert.assertTrue(time == server.getPingTime());

  }

  @Test
  public void isConnected() {
//    boolean connected = NetUtils.isConnected(server);
//    Assert.assertFalse(connected);
  }

  @Test
  public void avgPingTime() {
    Double pingTime = NetUtils.avgPingTime(server);
    Assert.assertNotNull(pingTime);
  }


}