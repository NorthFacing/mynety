package com.shadowsocks.client;

import com.shadowsocks.common.config.Constants;
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

/**
 * localServer接受到数据发送数据给remoteServer
 */
public final class SocksClientOutRelayHandler extends ChannelInboundHandlerAdapter {

	private static Logger logger = LoggerFactory.getLogger(SocksClientOutRelayHandler.class);

	private final ICrypt crypt;
	private final boolean isProxy;
	private final Channel relayChannel;

	public SocksClientOutRelayHandler(Channel relayChannel, boolean isProxy, ICrypt crypt) {
		this.crypt = crypt;
		this.isProxy = isProxy;
		this.relayChannel = relayChannel;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		try (ByteArrayOutputStream _remoteOutStream = new ByteArrayOutputStream()) {
			if (relayChannel.isActive()) {
				ByteBuf byteBuf = (ByteBuf) msg;
				if (!byteBuf.hasArray()) {
					int len = byteBuf.readableBytes();
					byte[] arr = new byte[len];
					byteBuf.getBytes(0, arr);

					if (isProxy) {
						crypt.encrypt(arr, arr.length, _remoteOutStream);
						arr = _remoteOutStream.toByteArray();
					}
					relayChannel.writeAndFlush(Unpooled.wrappedBuffer(arr));
					logger.info(Constants.LOG_MSG + ctx.channel() + " SendRemote message:isProxy = {},length = {}, channel = {}",
						isProxy, arr.length, relayChannel);
				}
			}
		} catch (Exception e) {
			logger.error(Constants.LOG_MSG + ctx.channel() + " Send data to remoteServer error: ", e);
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		if (relayChannel.isActive()) {
			SocksServerUtils.closeOnFlush(relayChannel);
		}
		logger.info(Constants.LOG_MSG + ctx.channel() + " OutRelay channelInactive close");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		logger.error(Constants.LOG_MSG + ctx.channel(), cause);
		ctx.close();
	}

}
