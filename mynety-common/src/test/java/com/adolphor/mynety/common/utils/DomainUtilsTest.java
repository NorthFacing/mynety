package com.adolphor.mynety.common.utils;

import com.adolphor.mynety.common.bean.Address;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static com.adolphor.mynety.common.constants.Constants.SCHEME_HTTP;
import static com.adolphor.mynety.common.constants.Constants.SCHEME_HTTPS;

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
  public void getAddressTest() throws Exception {

    String uri = "https://adolphor:666";
    Address address = DomainUtils.getAddress(uri);
    Assert.assertEquals("adolphor", address.getHost());
    Assert.assertEquals(666, address.getPort());

    uri = "185.199.108.153:443";
    address = DomainUtils.getAddress(uri);
    Assert.assertEquals("185.199.108.153", address.getHost());
    Assert.assertEquals(443, address.getPort());

    uri = "adolphor.com:80";
    address = DomainUtils.getAddress(uri);
    Assert.assertEquals("adolphor.com", address.getHost());
    Assert.assertEquals(80, address.getPort());

    uri = "https://git-scm.com/book/zh/v1/git";
    address = DomainUtils.getAddress(uri);
    Assert.assertEquals("git-scm.com", address.getHost());
    Assert.assertEquals(443, address.getPort());
    Assert.assertEquals(SCHEME_HTTPS, address.getScheme());
    Assert.assertEquals("/book/zh/v1/git", address.getPath());

    uri = "http://www.runoob.com/try/try.php?filename=tryjsref_regexp4";
    address = DomainUtils.getAddress(uri);
    Assert.assertEquals("www.runoob.com", address.getHost());
    Assert.assertEquals(80, address.getPort());
    Assert.assertEquals(SCHEME_HTTP, address.getScheme());

    uri = "http://adolphor.com:8080/";
    address = DomainUtils.getAddress(uri);
    Assert.assertEquals("adolphor.com", address.getHost());
    Assert.assertEquals(8080, address.getPort());
    Assert.assertEquals(SCHEME_HTTP, address.getScheme());
    Assert.assertEquals("/", address.getPath());

  }

}
