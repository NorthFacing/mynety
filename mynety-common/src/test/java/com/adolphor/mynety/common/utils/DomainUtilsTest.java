package com.adolphor.mynety.common.utils;

import com.adolphor.mynety.common.bean.Address;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static com.adolphor.mynety.common.constants.Constants.LOOPBACK_ADDRESS;
import static com.adolphor.mynety.common.constants.Constants.PORT_80;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class DomainUtilsTest {

  @Test
  public void regCheckTest() {

    ArrayList<String> list = new ArrayList<>();
    list.add("baidu.com");

    String domain = "baidu.com";
    boolean b = DomainUtils.regCheckForSubdomain(list, domain);
    Assert.assertTrue(b);

    domain = "abc.baidu.com";
    b = DomainUtils.regCheckForSubdomain(list, domain);
    Assert.assertTrue(b);

    domain = "google.com";
    b = DomainUtils.regCheckForSubdomain(list, domain);
    Assert.assertTrue(!b);

    domain = "ci6.googleusercontent.com";
    b = DomainUtils.regCheckForSubdomain(list, domain);
    Assert.assertTrue(!b);

    domain = "google.com.hk";
    b = DomainUtils.regCheckForSubdomain(list, domain);
    Assert.assertTrue(!b);

  }

  @Test
  public void RegTest() {
    String domain = "baidu.com";
    String reg = "([a-z0-9]+[.])*" + domain;
    boolean matches = Pattern.matches(reg, "ci6.googleusercontent.com");
    Assert.assertFalse(matches);
    domain = "googleusercontent.com";
    reg = "([a-z0-9]+[.])*" + domain;
    matches = Pattern.matches(reg, "ci6.googleusercontent.com");
    Assert.assertTrue(matches);
  }

  @Test
  public void getAddressTest() {

    String uri = "https://adolphor:666";
    Address address = DomainUtils.getAddress(httpReq(uri));
    Assert.assertEquals(LOOPBACK_ADDRESS, address.getHost());
    Assert.assertEquals(PORT_80, address.getPort());

    uri = "185.199.108.153";
    address = DomainUtils.getAddress(httpReq(uri));
    Assert.assertEquals("185.199.108.153", address.getHost());
    Assert.assertEquals(80, address.getPort());

    uri = "185.199.108.153:443";
    address = DomainUtils.getAddress(httpReq(uri));
    Assert.assertEquals("185.199.108.153", address.getHost());
    Assert.assertEquals(443, address.getPort());

    uri = "adolphor.com";
    address = DomainUtils.getAddress(httpReq(uri));
    Assert.assertEquals("adolphor.com", address.getHost());
    Assert.assertEquals(80, address.getPort());

    uri = "adolphor.com:80";
    address = DomainUtils.getAddress(httpReq(uri));
    Assert.assertEquals("adolphor.com", address.getHost());
    Assert.assertEquals(80, address.getPort());

    uri = "http://adolphor.com";
    address = DomainUtils.getAddress(httpReq(uri));
    Assert.assertEquals("adolphor.com", address.getHost());
    Assert.assertEquals(80, address.getPort());

    uri = "http://adolphor.com:8080";
    address = DomainUtils.getAddress(httpReq(uri));
    Assert.assertEquals("adolphor.com", address.getHost());
    Assert.assertEquals(8080, address.getPort());

    uri = "https://adolphor.com";
    address = DomainUtils.getAddress(httpReq(uri));
    Assert.assertEquals("adolphor.com", address.getHost());
    Assert.assertEquals(443, address.getPort());

    uri = "https://adolphor.com:666";
    address = DomainUtils.getAddress(httpReq(uri));
    Assert.assertEquals("adolphor.com", address.getHost());
    Assert.assertEquals(666, address.getPort());

  }

  private HttpRequest httpReq(String uri) {
    return new DefaultHttpRequest(HTTP_1_1, HttpMethod.CONNECT, uri);
  }

}
