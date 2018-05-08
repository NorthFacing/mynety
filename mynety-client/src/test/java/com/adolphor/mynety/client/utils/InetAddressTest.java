package com.adolphor.mynety.client.utils;

import com.adolphor.mynety.common.constants.Constants;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.util.regex.Matcher;

public class InetAddressTest {

  @Test
  public void test() throws Exception {
    Matcher matcher = Constants.IPV4_PATTERN.matcher("192.168.0.1");
    Assert.assertTrue(matcher.find());
    matcher = Constants.IPV6_PATTERN.matcher("2001:db8:0:1");
    Assert.assertTrue(matcher.find());


    InetAddress address = InetAddress.getByName("127.0.0.1");
    System.out.println(address.getHostName());
    System.out.println(address.getHostAddress());
  }

}
