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

import com.shadowsocks.client.httpAdapter.http_1_1.Http_1_1_Handler;
import com.shadowsocks.client.httpAdapter.tunnel.HttpTunnelHandler;
import com.shadowsocks.common.utils.SocksServerUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import static com.shadowsocks.common.constants.Constants.HTTP_REQUEST;
import static com.shadowsocks.common.constants.Constants.LOG_MSG;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * http 代理入口 请求分发，当前实现不加权限验证
 *
 * @author 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
@ChannelHandler.Sharable
public class HttpProxyHandler extends SimpleChannelInboundHandler<DefaultHttpRequest> {

  public static final HttpProxyHandler INSTANCE = new HttpProxyHandler();

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, DefaultHttpRequest httpRequest) throws Exception {
    ReferenceCountUtil.retain(httpRequest); // 增加引用计数，在下个处理器的active方法中进行消费
    ctx.channel().attr(HTTP_REQUEST).set(httpRequest);
    HttpVersion httpVersion = httpRequest.protocolVersion();
    if (HTTP_1_1 == httpVersion) {
      if (HttpMethod.CONNECT == httpRequest.method()) {
        ctx.pipeline().addLast(new HttpTunnelHandler());
      } else { // 除了connection，其余数据一律转发
        ctx.pipeline().addLast(new Http_1_1_Handler());
      }
    } else {
      logger.error("NOT SUPPORTED {} FOR NOW...", httpVersion);
      ctx.close();
    }
    ctx.pipeline().remove(this);
    logger.debug("{} {} remove handler: HttpProxyHandler", LOG_MSG, ctx.channel());
    ctx.pipeline().fireChannelActive();
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    logger.info("{} HttpProxyHandler channelReadComplete: {}", LOG_MSG, ctx.channel());
    ctx.flush();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
    logger.error(LOG_MSG + " HttpProxyHandler error: " + ctx.channel(), throwable);
    SocksServerUtils.closeOnFlush(ctx.channel());
  }


}
