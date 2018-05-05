package com.shadowsocks.client.utils;

import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.util.regex.Matcher;

import static com.shadowsocks.common.constants.Constants.IPV4_PATTERN;
import static com.shadowsocks.common.constants.Constants.IPV6_PATTERN;

public class InetAddressTest {

  @Test
  public void test() throws Exception {
    Matcher matcher = IPV4_PATTERN.matcher("192.168.0.1");
    Assert.assertTrue(matcher.find());
    matcher = IPV6_PATTERN.matcher("2001:db8:0:1");
    Assert.assertTrue(matcher.find());


    InetAddress address = InetAddress.getByName("127.0.0.1");
    System.out.println(address.getHostName());
    System.out.println(address.getHostAddress());
  }

}
