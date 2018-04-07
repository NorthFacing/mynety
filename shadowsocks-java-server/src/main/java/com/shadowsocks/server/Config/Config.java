package com.shadowsocks.server.Config;

import com.shadowsocks.common.encryption.ICrypt;
import io.netty.buffer.ByteBuf;
import io.netty.util.AttributeKey;

public class Config {

	public static int LOCAL_PORT = 1086;
	public static String METHOD;
	public static String PASSWORD;

	public static final AttributeKey<ICrypt> CRYPT_KEY = AttributeKey.valueOf("crypt");
	public static final AttributeKey<String> HOST = AttributeKey.valueOf("host");
	public static final AttributeKey<Integer> PORT = AttributeKey.valueOf("port");
	public static final AttributeKey<ByteBuf> BUF = AttributeKey.valueOf("buf");

}
