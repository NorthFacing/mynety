package com.adolphor.mynety.client;

import com.adolphor.mynety.common.wrapper.AbstractSimpleHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
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

import static com.adolphor.mynety.common.constants.Constants.ATTR_SOCKS5_REQUEST;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG_IN;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * 权限验证处理器
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
@ChannelHandler.Sharable
public final class AuthHandler extends AbstractSimpleHandler<SocksMessage> {

  public static final AuthHandler INSTANCE = new AuthHandler();

  @Override
  public void channelRead0(ChannelHandlerContext ctx, SocksMessage socksRequest) throws Exception {
    switch (socksRequest.version()) {
      // Socks5代理则可以支持TCP和UDP两种应用
      case SOCKS5:
        if (socksRequest instanceof Socks5InitialRequest) {
          logger.info("[ {}{} ] SOCKS5 auth first request...", ctx.channel().id(), LOG_MSG);
          // 不需要auth验证的代码范例
          List<Socks5AuthMethod> methods = ((Socks5InitialRequest) socksRequest).authMethods();
          if (methods.contains(Socks5AuthMethod.NO_AUTH)) {
            // Socks5CommandRequestDecoder 负责解码接下来会收到的 Command 请求
            ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
            logger.info("[ {}{} ] add handler: Socks5CommandRequestDecoder", ctx.channel().id(), LOG_MSG);
            // 给客户端发送 NOAUTH 的响应
            ctx.writeAndFlush(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
            logger.info("[ {}{} ] socks response for handsShake: Socks5AuthMethod.NO_AUTH", ctx.channel().id(), LOG_MSG_IN);
          } else { // 只接受无密码连接
            ctx.writeAndFlush(new DefaultSocks5InitialResponse(Socks5AuthMethod.UNACCEPTED));
            logger.info("[ {}{} ] socks response for handsShake: Socks5AuthMethod.UNACCEPTED", ctx.channel().id(), LOG_MSG_IN);
          }
          // auth验证的代码范例
//					ctx.pipeline().addFirst(new Socks5PasswordAuthRequestDecoder());
//					ctx.writeAndFlush(new DefaultSocks5AuthMethodResponse(Socks5AuthMethod.PASSWORD));
        } else if (socksRequest instanceof Socks5PasswordAuthRequest) {
          logger.warn("[ {}{} ] socks response for handsShake: Socks5AuthMethod.UNACCEPTED", ctx.channel().id(), LOG_MSG);
          ctx.close();
//					ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
//					ctx.writeAndFlush(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
        } else if (socksRequest instanceof Socks5CommandRequest) {
          logger.warn("[ {}{} ] SOCKS5 command request...", ctx.channel(), LOG_MSG);
          Socks5CommandRequest socks5CmdRequest = (Socks5CommandRequest) socksRequest;
          if (socks5CmdRequest.type() == Socks5CommandType.CONNECT) {
            logger.warn("[ {}{} ] SOCKS5 command request is CONNECT, need to executing connection handler...", ctx.channel().id(), LOG_MSG);
            ctx.channel().attr(ATTR_SOCKS5_REQUEST).set(socks5CmdRequest);
            ctx.pipeline().addLast(InBoundHandler.INSTANCE);
            logger.info("[ {}{} ] add handler: InBoundHandler", ctx.channel().id(), LOG_MSG);
            ctx.pipeline().remove(this);
            logger.info("[ {}{} ] remove handler: AuthHandler", ctx.channel().id(), LOG_MSG);
            ctx.pipeline().fireChannelActive(); // 通知执行下一个InboundHandler，也就是ConnectHandler
          } else {
            logger.warn("[ {}{} ] SOCKS5 command request is not CONNECT...", ctx.channel().id(), LOG_MSG);
            ctx.close();
          }
        } else {
          logger.warn("[ {}{} ] unsupported SOCKS5 command type: {}", ctx.channel().id(), LOG_MSG, getSimpleName(socksRequest));
          ctx.close();
        }
        break;
      case UNKNOWN:
        ctx.close();
        break;
      default:
        ctx.close();
    }
  }

}
