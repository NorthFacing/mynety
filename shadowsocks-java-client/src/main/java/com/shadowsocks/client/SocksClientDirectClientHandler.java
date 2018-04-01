package com.shadowsocks.client;

import com.shadowsocks.common.config.Constants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SocksClientDirectClientHandler extends ChannelInboundHandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(SocksClientDirectClientHandler.class);

	private final Promise<Channel> promise;

	public SocksClientDirectClientHandler(Promise<Channel> promise) {
		this.promise = promise;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		logger.info(Constants.LOG_MSG + ctx.channel());
		ctx.pipeline().remove(this); // 完成任务，从 pipeline 中移除
		promise.setSuccess(ctx.channel()); // 连接到指定地址成功后，setSuccess 让 Promise 的回调函数执行；在这个 Promise 中放有一个连接远程的 Channel
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
		logger.error(Constants.LOG_MSG, throwable);
		promise.setFailure(throwable);
	}
}
