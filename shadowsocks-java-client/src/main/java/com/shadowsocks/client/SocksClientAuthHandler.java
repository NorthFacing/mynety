package com.shadowsocks.client;

import com.shadowsocks.common.config.Constants;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ChannelHandler.Sharable // 线程安全
public final class SocksClientAuthHandler extends SimpleChannelInboundHandler<SocksMessage> {

	private static final Logger logger = LoggerFactory.getLogger(SocksClientAuthHandler.class);

	public static final SocksClientAuthHandler INSTANCE = new SocksClientAuthHandler(); // 因为线程安全，所以只需要初始化一个实例即可

	private SocksClientAuthHandler() {
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, SocksMessage socksRequest) throws Exception {
		switch (socksRequest.version()) {
			case SOCKS5: // Socks5代理则可以支持TCP和UDP两种应用

				if (socksRequest instanceof Socks5InitialRequest) {
					logger.info(Constants.LOG_MSG + ctx.channel() + " SOCKS5 auth first request, return Socks5AuthMethod.NO_AUTH");
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
					logger.error(Constants.LOG_MSG + ctx.channel() + " SOCKS5 auth request, 本客户端不需要密码连接！");
					ctx.close();
//					ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
//					ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
				} else if (socksRequest instanceof Socks5CommandRequest) {
					logger.info(Constants.LOG_MSG + ctx.channel() + " SOCKS5 command request, return Socks5AuthMethod.NO_AUTH");
					Socks5CommandRequest socks5CmdRequest = (Socks5CommandRequest) socksRequest;
					if (socks5CmdRequest.type() == Socks5CommandType.CONNECT) {
						ctx.pipeline().addLast(new SocksClientConnectHandler());
						ctx.pipeline().remove(this); // 完成任务，从 pipeline 中移除
						ctx.fireChannelRead(socksRequest); // 通知执行下一个InboundHandler
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
		logger.info(Constants.LOG_MSG + ctx.channel());
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
		logger.error(Constants.LOG_MSG + ctx.channel(), throwable);
		SocksServerUtils.closeOnFlush(ctx.channel());
	}
}
