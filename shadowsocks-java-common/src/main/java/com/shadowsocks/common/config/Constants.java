package com.shadowsocks.common.config;

import java.util.HashMap;
import java.util.Map;

public class Constants {

	public static final String METHOD = "method";
	public static final String PASSWORD = "password";

	public static final String LOCALPORT = "localPort";

	public static final String HOST = "host";
	public static final String PORT = "port";

	// 存放配置结果的容器
	public static final Map<String, String> config = new HashMap<>();

	public static final String LOG_MSG = "-----------> ";

}
