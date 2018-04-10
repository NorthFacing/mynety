package com.shadowsocks.client.config;

import com.shadowsocks.common.encryption.ICrypt;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.apache.commons.net.telnet.TelnetClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServerConfig {

	private static final Logger logger = LoggerFactory.getLogger(ServerConfig.class);

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

	public static void addServer(Server server) {
		servers.add(server);
	}

	public static Server getServer() {
		if (servers.size() == 0) {
			return null;
		}
		for (Server server : servers) {
			if (server.isAvailable()) {
				logger.debug("获取代理服务器成功：{}", server);
				return server;
			}
		}
		return null;
	}

	/**
	 * 检测服务器可用性
	 */
	public static void checkServers() {
		Executors.newScheduledThreadPool(1)
			.scheduleWithFixedDelay(
				() -> servers.forEach(server -> {
					boolean isAvailable = isConnected(server);
					logger.info("服务器检测：{}:{} => {}", server.getHost(), server.getPort(), isAvailable);
					server.setAvailable(isAvailable);
				}),
				0,
				30,
				TimeUnit.SECONDS
			);
	}


	/**
	 * telnet 检测是否能连通
	 *
	 * @param server 服务器实例
	 */
	private static boolean isConnected(Server server) {
		try {
			TelnetClient client = new TelnetClient();
			client.setDefaultTimeout(3000);
			client.connect(server.getHost(), server.getPort());
			return true;
		} catch (Exception e) {
			logger.warn("remote server: " + server.toString() + " telnet failed");
		}
		return false;
	}


}