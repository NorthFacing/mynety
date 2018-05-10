package com.adolphor.mynety.common.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.regex.Pattern;

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


}
