package com.shadowsocks.client.utils;

import com.shadowsocks.client.config.PacConfig;
import com.shadowsocks.client.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PacFilter {

	private static final Logger logger = LoggerFactory.getLogger(PacFilter.class);

	/**
	 * 判断是否需要进行代理
	 *
	 * @param domain 需要判断的域名
	 * @return 需要代理返回 true，否则 false
	 */
	public static boolean isProxy(String domain) {
		int strategy = ServerConfig.PROXY_STRATEGY;
		switch (strategy) {
			case 0: // 全局
				return true;
			case 1: // PAC优先代理模式下，使用直连的域名
				return !regCheck(PacConfig.directDomains, domain);
			case 2: // PAC优先直连模式下，使用代理的域名
				return regCheck(PacConfig.proxyDomains, domain);
			default: // 默认开启全局
				return true;
		}
	}

	/**
	 * 正则验证：校验域名是否需要存在于配置的列表中
	 *
	 * @param confList 配置的域名集合
	 * @param domain   需要校验的域名
	 * @return domain正则匹配到confList中的元素就返回true，否则false
	 */
	private static boolean regCheck(List<String> confList, String domain) {
		try {
			long match = confList.parallelStream()
				.filter(conf -> Pattern.matches("^(\\w?.?)+" + conf, domain))
				.count();
			return match > 0 ? true : false;
		} catch (Exception e) {
			logger.error("域名验证出错：{}", e);
			return false;
		}
	}

	public static void main(String[] args) throws Exception {
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
