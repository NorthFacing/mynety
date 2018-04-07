package com.shadowsocks.server.Config;

import lombok.Data;

@Data
public class ServerConfig {

	public static final ServerConfig config = new ServerConfig();

	private int localPort;
	private String method;
	private String password;

}
