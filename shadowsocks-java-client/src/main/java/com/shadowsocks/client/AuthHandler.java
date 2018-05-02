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
package com.shadowsocks.client;

import com.shadowsocks.common.utils.SocksServerUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialResponse;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequest;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;
import static com.shadowsocks.common.constants.Constants.SOCKS5_REQUEST;

/**
 * 权限验证处理器
 *
 * @author 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Slf4j
@ChannelHandler.Sharable // 线程安全
public final class AuthHandler extends SimpleChannelInboundHandler<SocksMessage> {

  public static final AuthHandler INSTANCE = new AuthHandler(); // 因为线程安全，所以只需要初始化一个实例即可

  private AuthHandler() {
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, SocksMessage socksRequest) throws Exception {
    switch (socksRequest.version()) {
      case SOCKS5: // Socks5代理则可以支持TCP和UDP两种应用

        if (socksRequest instanceof Socks5InitialRequest) {
          logger.info(LOG_MSG + ctx.channel() + " SOCKS5 auth first request, return Socks5AuthMethod.NO_AUTH");
          // 不需要auth验证的代码范例
          List<Socks5AuthMethod> methods = ((Socks5InitialRequest) socksRequest).authMethods();
          if (methods.contains(Socks5AuthMethod.NO_AUTH)) {
            ctx.pipeline().addFirst(new Socks5CommandRequestDecoder()); // Socks5CommandRequestDecoder 负责解码接下来会收到的 Command 请求
            ctx.write(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH)); // 给客户端发送了采用 NOAUTH 的响应
          } else { // 只接受无密码连接
            ctx.write(new DefaultSocks5InitialResponse(Socks5AuthMethod.UNACCEPTED));
          }
          // auth验证的代码范例
//					ctx.pipeline().addFirst(new Socks5PasswordAuthRequestDecoder());
//					ctx.write(new DefaultSocks5AuthMethodResponse(Socks5AuthMethod.PASSWORD));
        } else if (socksRequest instanceof Socks5PasswordAuthRequest) {
          logger.error(LOG_MSG + ctx.channel() + " SOCKS5 auth request, 本客户端不需要密码连接！");
          ctx.close();
//					ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
//					ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
        } else if (socksRequest instanceof Socks5CommandRequest) {
          logger.info(LOG_MSG + ctx.channel() + " SOCKS5 command request...");
          Socks5CommandRequest socks5CmdRequest = (Socks5CommandRequest) socksRequest;
          if (socks5CmdRequest.type() == Socks5CommandType.CONNECT) {
            ctx.channel().attr(SOCKS5_REQUEST).set(socks5CmdRequest);
            ctx.pipeline().remove(this); // 完成任务，从 pipeline 中移除
            ctx.pipeline().addLast(new ConnectHandler());
            ctx.pipeline().fireChannelActive(); // 通知执行下一个InboundHandler，也就是ConnectHandler
          } else {
            ctx.close();
          }
        } else {
          ctx.close();
        }
        break;
      case UNKNOWN:
        ctx.close();
        break;
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    logger.info(LOG_MSG + ctx.channel());
    ctx.flush();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
    logger.error(LOG_MSG + ctx.channel(), throwable);
    SocksServerUtils.closeOnFlush(ctx.channel());
  }
}
