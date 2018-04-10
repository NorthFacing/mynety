package com.shadowsocks.client.config;

import com.shadowsocks.common.encryption.ICrypt;
import lombok.Data;

@Data
public class Server {

	private String remarks; // 服务器备注名称
	private String host;
	private int port;
	private String method;
	private String password;
	private ICrypt crypt;
	private boolean available = true; // 是否可用

}
