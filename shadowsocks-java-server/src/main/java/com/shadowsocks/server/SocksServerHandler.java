package com.shadowsocks.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socks.SocksAuthResponse;
import io.netty.handler.codec.socks.SocksAuthScheme;
import io.netty.handler.codec.socks.SocksAuthStatus;
import io.netty.handler.codec.socks.SocksCmdRequest;
import io.netty.handler.codec.socks.SocksCmdRequestDecoder;
import io.netty.handler.codec.socks.SocksCmdType;
import io.netty.handler.codec.socks.SocksInitResponse;
import io.netty.handler.codec.socks.SocksRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SocksServerHandler extends SimpleChannelInboundHandler<SocksRequest> {

	private static Logger logger = LoggerFactory.getLogger(SocksServerHandler.class);

	private Map<String, String> config;

	public SocksServerHandler(Map<String, String> config) {
		this.config = config;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, SocksRequest socksRequest) throws Exception {
		switch (socksRequest.requestType()) {
			case INIT: {
				logger.info("localserver init");
				ctx.pipeline().addFirst(new SocksCmdRequestDecoder());
				ctx.write(new SocksInitResponse(SocksAuthScheme.NO_AUTH));
				break;
			}
			case AUTH:
				logger.info("localserver auth");
				ctx.pipeline().addFirst(new SocksCmdRequestDecoder());
				ctx.write(new SocksAuthResponse(SocksAuthStatus.SUCCESS));
				break;
			case CMD:
				logger.info("localserver cmd");
				SocksCmdRequest req = (SocksCmdRequest) socksRequest;
				if (req.cmdType() == SocksCmdType.CONNECT) {
					logger.info("localserver connect");
//					ctx.pipeline().addLast(new SocksServerConnectHandler(config));
					ctx.pipeline().remove(this);
					ctx.fireChannelRead(socksRequest);
				} else {
					ctx.close();
					logger.error("localserver cmd => {}", req.cmdType());
				}
				break;
			case UNKNOWN:
				logger.error("localserver unknown");
				ctx.close();
				break;
		}
	}
}
