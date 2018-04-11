package com.shadowsocks.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;

public final class RemoteHandler extends ChannelInboundHandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(RemoteHandler.class);

	private final Promise<Channel> promise;

	public RemoteHandler(Promise<Channel> promise) {
		this.promise = promise;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		logger.info(LOG_MSG + " 将 inboundChannel eventLoop channel={} 和 promise={} 进行关联", ctx.channel(), promise.getNow());
		ctx.pipeline().remove(this);
		promise.setSuccess(ctx.channel()); // 连接到指定地址成功后，setSuccess 让 Promise 的回调函数执行；在这个 Promise 中放有一个连接远程的 Channel
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
		logger.error(LOG_MSG + " inboundChannel=" + ctx.channel() + " 和 outboundChannel=" + promise.getNow() + " 关联出错：", throwable);
		promise.setFailure(throwable);
	}
}
