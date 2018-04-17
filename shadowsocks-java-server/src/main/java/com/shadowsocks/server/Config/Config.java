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
package com.shadowsocks.server.Config;

import com.shadowsocks.common.encryption.ICrypt;
import io.netty.buffer.ByteBuf;
import io.netty.util.AttributeKey;

/**
 * 服务端配置
 *
 * @author 0haizhu0@gmail.com
 * @since v0.0.1
 */
public class Config {

  public static int LOCAL_PORT = 1086;
  public static String METHOD;
  public static String PASSWORD;

  public static final AttributeKey<ICrypt> CRYPT_KEY = AttributeKey.valueOf("crypt");
  public static final AttributeKey<String> HOST = AttributeKey.valueOf("host");
  public static final AttributeKey<Integer> PORT = AttributeKey.valueOf("port");
  public static final AttributeKey<ByteBuf> BUF = AttributeKey.valueOf("buf");

}
