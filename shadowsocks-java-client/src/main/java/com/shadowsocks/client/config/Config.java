package com.shadowsocks.client.config;

import java.util.ArrayList;
import java.util.List;

public class Config {

	public static String LOCAL_HOST = "0.0.0.0";
	public static int LOCAL_PORT = 1086;

	public static final List<Server> servers = new ArrayList<>();
	private static Server server;

	// TODO 动态选择
	public static Server getServer() {
		server = servers.get(0);
		return server;
	}

}