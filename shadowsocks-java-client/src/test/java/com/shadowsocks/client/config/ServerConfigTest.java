package com.shadowsocks.client.config;

import org.junit.Assert;
import org.junit.Test;

public class ServerConfigTest {

  @Test
  public void getAvailableServer() {
    double time = 100d;
    for (int i = 0; i < 5; i++) {
      Server server = new Server();
      server.setPingTime(time + i);
      ServerConfig.servers.add(server);
    }

    Server server = ServerConfig.getAvailableServer();
    Assert.assertTrue(time == server.getPingTime());

  }
}