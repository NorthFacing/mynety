package com.adolphor.mynety.common.constants;

import com.adolphor.mynety.common.bean.Address;
import com.adolphor.mynety.common.encryption.ICrypt;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.util.AttributeKey;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 常量
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.1
 */
public class Constants {

  public static final String LOOPBACK_ADDRESS = "127.0.0.1";
  public static final String ALL_LOCAL_ADDRESS = "0.0.0.0";


  public static final String LOG_MSG = " <==> ";
  public static final String LOG_MSG_OUT = " >>> ";
  public static final String LOG_MSG_IN = " <<< ";

  public static Class channelClass;
  public static Class serverChannelClass;
  public static Class bossGroupClass;
  public static Class workerGroupClass;

  public static final Pattern PATH_PATTERN = Pattern.compile("(https?)://([a-zA-Z0-9\\.\\-]+)(:(\\d+))?(/.*)");
  public static final Pattern TUNNEL_ADDR_PATTERN = Pattern.compile("^([a-zA-Z0-9\\.\\-_]+):(\\d+)");

  public static final Pattern IPV4_PATTERN = Pattern.compile("(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])");
  public static final Pattern IPV6_PATTERN = Pattern.compile("([0-9a-f]{1,4}:){3}([0-9a-f]){1,4}");

  public static final AttributeKey<ICrypt> ATTR_CRYPT_KEY = AttributeKey.valueOf("crypt");

  public static final AttributeKey<Address> REQUEST_ADDRESS = AttributeKey.valueOf("request.address");
  public static final AttributeKey<List> REQUEST_TEMP_LIST = AttributeKey.valueOf("request.temp.list");

  public static final HttpResponseStatus CONNECTION_ESTABLISHED = new HttpResponseStatus(HttpResponseStatus.OK.code(), "Connection established");
  public static final AttributeKey<Boolean> IS_KEEP_ALIVE = AttributeKey.valueOf("keep.alive");


  public static final AttributeKey<Socks5CommandRequest> SOCKS5_REQUEST = AttributeKey.valueOf("socks5.request");

}
