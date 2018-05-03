/**
 * MIT License
 * <p>
 * Copyright (c) 2018 0haizhu0@gmail.com
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.shadowsocks.common.constants;

import com.shadowsocks.common.bean.FullPath;
import com.shadowsocks.common.encryption.ICrypt;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.util.AttributeKey;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 常量
 *
 * @author 0haizhu0@gmail.com
 * @since v0.0.1
 */
public class Constants {

  public static final String LOG_MSG = "-----------> ";

  public static final Pattern PATH_PATTERN = Pattern.compile("(https?)://([a-zA-Z0-9\\.\\-]+)(:(\\d+))?(/.*)");
  public static final Pattern TUNNEL_ADDR_PATTERN = Pattern.compile("^([a-zA-Z0-9\\.\\-_]+):(\\d+)");

  public static final Pattern IPV4_PATTERN = Pattern.compile("(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])");
  public static final Pattern IPV6_PATTERN = Pattern.compile("([0-9a-f]{1,4}:){3}([0-9a-f]){1,4}");

  public static final HttpResponseStatus CONNECTION_ESTABLISHED = new HttpResponseStatus(HttpResponseStatus.OK.code(), "Connection established");

  public static Class channelClass;
  public static Class serverChannelClass;
  public static Class bossGroupClass;
  public static Class workerGroupClass;

  public static final AttributeKey<ICrypt> ATTR_CRYPT_KEY = AttributeKey.valueOf("crypt");
  public static final AttributeKey<String> ATTR_HOST = AttributeKey.valueOf("host");
  public static final AttributeKey<Integer> ATTR_PORT = AttributeKey.valueOf("port");
  public static final AttributeKey<ByteBuf> ATTR_BUF = AttributeKey.valueOf("buf");

  public static final AttributeKey<Socks5CommandRequest> SOCKS5_REQUEST = AttributeKey.valueOf("socks5.request");
  public static final AttributeKey<DefaultHttpRequest> HTTP_REQUEST = AttributeKey.valueOf("http.request");
  public static final AttributeKey<FullPath> HTTP_REQUEST_FULLPATH = AttributeKey.valueOf("http.request.fullPath");
  public static final AttributeKey<Boolean> SOCKS5_CONNECTED = AttributeKey.valueOf("socks.connected");
  public static final AttributeKey<List<ChannelHandler>> EXTRA_OUT_RELAY_HANDLER = AttributeKey.valueOf("extra.out.relay.handler");

}
