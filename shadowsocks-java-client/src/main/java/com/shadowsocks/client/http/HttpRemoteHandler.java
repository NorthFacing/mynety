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
package com.shadowsocks.client.http;

import com.shadowsocks.client.http.tunnel.HttpTunnelConnectionHandler;
import com.shadowsocks.common.nettyWrapper.AbstractInRelayHandler;
import com.shadowsocks.common.nettyWrapper.AbstractOutRelayHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import static com.shadowsocks.common.constants.Constants.CONNECTION_ESTABLISHED;
import static com.shadowsocks.common.constants.Constants.LOG_MSG;
import static com.shadowsocks.common.constants.Constants.LOG_MSG_IN;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * http 代理模式下 远程处理器，连接真正的目标地址
 * <p>
 * 本类中不指定数据类型，数据类型和编解码器都由 inRelay 处理器那里指定
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public class HttpRemoteHandler extends AbstractOutRelayHandler<Object> {

  private AbstractInRelayHandler inRelayHandler;

  public HttpRemoteHandler(AbstractInRelayHandler inRelayHandler, Channel clientProxyChannel) {
    super(clientProxyChannel);
    this.inRelayHandler = inRelayHandler;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    Channel remoteClient = ctx.channel();
    super.channelActive(ctx);

    // 连接成功
    inRelayHandler.setConnected(true);

    // 如果是tunnel连接，则告诉客户端建立隧道成功（直接将后续数据进行转发）
    if (inRelayHandler instanceof HttpTunnelConnectionHandler) {
      DefaultHttpResponse response = new DefaultHttpResponse(HTTP_1_1, CONNECTION_ESTABLISHED);
      clientChannel.writeAndFlush(response);
      logger.debug("[ {}{}{} ] httpTunnel connect socks success, write response to user-agent: {}", clientChannel, LOG_MSG, remoteClient, response);
    }

    // 移除 inbound 和 outbound 双方的编解码(tunnel代理如果没有增加ssl解析，那么就必须移除HTTP编解码器)
    clientChannel.pipeline().remove(HttpServerCodec.class);
    logger.debug("[ {}{}{} ] clientChannel remove handler: HttpServerCodec", clientChannel, LOG_MSG, remoteClient);
    remoteClient.pipeline().remove(HttpClientCodec.class);
    logger.debug("[ {}{}{} ] remoteChannel remove handler: HttpClientCodec", clientChannel, LOG_MSG, remoteClient);
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, Object msg) {
    logger.debug("[ {}{}{} ] channelRead: {}", clientChannel, LOG_MSG_IN, ctx.channel(), msg);
    if (!clientChannel.isOpen()) {
      channelClose(ctx);
      return;
    }
    try {
      ReferenceCountUtil.retain(msg);
      clientChannel.writeAndFlush(msg);
      logger.debug("[ {}{}{} ] write to user-agent channel: {}", clientChannel, LOG_MSG_IN, ctx.channel(), msg);
    } catch (Exception e) {
      logger.error("[ " + clientChannel + LOG_MSG_IN + ctx.channel() + " ] error", e);
      channelClose(ctx);
    }
  }

}

