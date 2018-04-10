package com.shadowsocks.client.config;

import com.shadowsocks.common.encryption.ICrypt;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.ArrayList;
import java.util.List;

public class ServerConfig {

	public static boolean PUBLIC = true;

	public static int LOCAL_PORT = 1086;

	/**
	 * 代理模式可选项：
	 * 0：全局
	 * 1：优先代理
	 * 2：优先本地
	 */
	public static int PROXY_STRATEGY = 0;

	private static final List<Server> servers = new ArrayList<>();

	public static final AttributeKey<ICrypt> CRYPT_KEY = AttributeKey.valueOf("crypt");
	public static final AttributeKey<Boolean> IS_PROXY = AttributeKey.valueOf("isProxy");
	public static final AttributeKey<String> DST_ADDR = AttributeKey.valueOf("dstAddr");
	public static final AttributeKey<Channel> CLIENT_CHANNEL = AttributeKey.valueOf("clientChannel");
	public static final AttributeKey<Channel> REMOTE_CHANNEL = AttributeKey.valueOf("remoteChannel");

	public static void addServer(Server server){
		servers.add(server);
	}

	public static Server getServer() {
		return servers.get(0);
	}

}