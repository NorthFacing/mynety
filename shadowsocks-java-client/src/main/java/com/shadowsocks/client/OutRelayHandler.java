package com.shadowsocks.client;

import com.shadowsocks.client.config.ServerConfig;
import com.shadowsocks.common.encryption.ICrypt;
import com.shadowsocks.common.utils.SocksServerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;

/**
 * localServer接受到数据发送数据给remoteServer
 */
public final class OutRelayHandler extends ChannelInboundHandlerAdapter {

	private static Logger logger = LoggerFactory.getLogger(OutRelayHandler.class);

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		Channel remoteChannel = ctx.channel().attr(ServerConfig.REMOTE_CHANNEL).get();
		Boolean isProxy = ctx.channel().attr(ServerConfig.IS_PROXY).get();
		ICrypt crypt = ctx.channel().attr(ServerConfig.CRYPT_KEY).get();
		String dstAddr = remoteChannel.attr(ServerConfig.DST_ADDR).get();

		try (ByteArrayOutputStream _remoteOutStream = new ByteArrayOutputStream()) {
			if (remoteChannel.isActive()) {

				logger.info(LOG_MSG + " 是否使用代理：" + dstAddr + " => " + isProxy);

				ByteBuf byteBuf = (ByteBuf) msg;
				if (!byteBuf.hasArray()) {
					int len = byteBuf.readableBytes();
					byte[] arr = new byte[len];
					byteBuf.getBytes(0, arr);

					if (isProxy) {
						crypt.encrypt(arr, arr.length, _remoteOutStream);
						arr = _remoteOutStream.toByteArray();
					}
					remoteChannel.writeAndFlush(Unpooled.wrappedBuffer(arr));

					logger.debug(LOG_MSG + ctx.channel() + " SendRemote message:isProxy = {},length = {}, channel = {}",
						isProxy, arr.length, remoteChannel);
				}
			}
		} catch (Exception e) {
			logger.error(LOG_MSG + ctx.channel() + " Send data to remoteServer error: ", e);
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		Channel remoteChannel = ctx.channel().attr(ServerConfig.REMOTE_CHANNEL).get();
		if (remoteChannel.isActive()) {
			SocksServerUtils.closeOnFlush(remoteChannel);
		}
		logger.info(LOG_MSG + ctx.channel() + " OutRelay channelInactive close");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		logger.error(LOG_MSG + ctx.channel(), cause);
		ctx.close();
	}

}
