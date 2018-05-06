/**
 * MIT License
 * <p>
 * Copyright (c) Bob.Zhu
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

import com.shadowsocks.client.httpAdapter.http_1_1.Http_1_1_2Socks5Handler;
import com.shadowsocks.client.httpAdapter.tunnel.HttpTunnel2Socks5Handler;
import com.shadowsocks.client.socks5Wrapper.SocksWrapperHandsShakeHandler;
import com.shadowsocks.common.nettyWrapper.TempAbstractInRelayHandler;
import com.shadowsocks.common.utils.SocksServerUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import lombok.extern.slf4j.Slf4j;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;

/**
 * http 代理模式下 远程连接处理器列表
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public class HttpOutboundInitializer extends ChannelInitializer<SocketChannel> {

  private Channel clientChannel;
  private TempAbstractInRelayHandler inRelayhandler;

  public HttpOutboundInitializer(TempAbstractInRelayHandler inRelayhandler, Channel clientChannel) {
    this.clientChannel = clientChannel;
    this.inRelayhandler = inRelayhandler;
  }

  @Override
  @SuppressWarnings("Duplicates")
  protected void initChannel(SocketChannel ch) throws Exception {
    // HTTP tunnel
    if (inRelayhandler instanceof HttpTunnel2Socks5Handler) {
      ch.pipeline().addLast(new SocksWrapperHandsShakeHandler(inRelayhandler, clientChannel));
      logger.info("[ {}{}{} ] http tunnel out pipeline add handlers: SocksWrapperHandsShakeHandler", clientChannel, LOG_MSG, ch);
      ch.pipeline().addLast(new HttpClientCodec());
      logger.info("[ {}{}{} ] http tunnel out pipeline add handlers: HttpClientCodec", clientChannel, LOG_MSG, ch);
      ch.pipeline().addLast(new HttpRemoteHandler(clientChannel));
      logger.info("[ {}{}{} ] http tunnel out pipeline add handlers: HttpRemoteHandler", clientChannel, LOG_MSG, ch);
    }
    // HTTP1.1
    else if (inRelayhandler instanceof Http_1_1_2Socks5Handler) {
      ch.pipeline().addLast(new SocksWrapperHandsShakeHandler(inRelayhandler, clientChannel));
      logger.info("[ {}{}{} ] http1.1 out pipeline add handlers: SocksWrapperHandsShakeHandler", clientChannel, LOG_MSG, ch);
      ch.pipeline().addLast(new HttpClientCodec());
      logger.info("[ {}{}{} ] http1.1 out pipeline add handlers: HttpClientCodec", clientChannel, LOG_MSG, ch);
      ch.pipeline().addLast(new HttpRemoteHandler(clientChannel));
      logger.info("[ {}{}{} ] http1.1 out pipeline add handlers: HttpRemoteHandler", clientChannel, LOG_MSG, ch);
    }
    // to be supported...
    else {
      logger.error("[ {}{}{} ] unhandled inRelayhandler: {}", clientChannel, LOG_MSG, ch, inRelayhandler.getClass().getSimpleName());
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    logger.error("[ " + ctx.channel() + LOG_MSG + "] error: ", cause);
    SocksServerUtils.flushOnClose(ctx.channel());
  }

}
