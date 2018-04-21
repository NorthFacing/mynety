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
package com.shadowsocks.client.httpAdapter;

import com.shadowsocks.common.constants.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

/**
 * @author 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public class MyConnectionSocketFactory extends SSLConnectionSocketFactory {

  public MyConnectionSocketFactory(final SSLContext sslContext) {
    super(sslContext);
  }

  @Override
  public Socket createSocket(final HttpContext context) throws IOException {
    InetSocketAddress socksaddr = (InetSocketAddress) context.getAttribute(Constants.SOCKS_ADDR_FOR_HTTP);
    Proxy proxy = new Proxy(Proxy.Type.SOCKS, socksaddr);
    return new Socket(proxy);
  }

}