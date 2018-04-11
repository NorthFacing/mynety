package com.shadowsocks.client.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

import static com.shadowsocks.client.utils.PacFilter.regCheck;

public class PacFilterTest {

	@Test
	public void regCheckTest() {

		ArrayList<String> list = new ArrayList<>();
		list.add("baidu.com");

		String domain = "www.baidu.com";
		boolean b = regCheck(list, domain);
		Assert.assertTrue(b);

		domain = "abc.baidu.com";
		b = regCheck(list, domain);
		Assert.assertTrue(b);

		domain = "baidu.com.cn";
		b = regCheck(list, domain);
		Assert.assertTrue(!b);

		domain = "google.com";
		b = regCheck(list, domain);
		Assert.assertTrue(!b);

		domain = "abc.google.com";
		b = regCheck(list, domain);
		Assert.assertTrue(!b);

		domain = "google.com.hk";
		b = regCheck(list, domain);
		Assert.assertTrue(!b);

	}


}
