package com.shadowsocks.client.config;

import lombok.Data;

@Data
public class Server {

	private String remarks; // 服务器备注名称
	private String host;
	private int port;
	private String method;
	private String password;
	private long ping = -1; // ping 时长（选择ping时间最短的服务器）

}
