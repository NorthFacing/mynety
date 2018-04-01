package com.shadowsocks.client;

import com.shadowsocks.common.config.Constants;
import com.shadowsocks.common.encryption.ICrypt;
import com.shadowsocks.common.utils.SocksServerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;

/**
 * 接受remoteServer的数据，发送给客户端
 */
public final class SocksClientInRelayHandler extends ChannelInboundHandlerAdapter {

	private static Logger logger = LoggerFactory.getLogger(SocksClientInRelayHandler.class);

	private final ICrypt crypt;
	private final boolean isProxy;
	private final Channel relayChannel;

	public SocksClientInRelayHandler(Channel relayChannel, boolean isProxy, ICrypt crypt) {
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
		ByteBuf byteBuf = (ByteBuf) msg;
		logger.info(Constants.LOG_MSG + ctx.channel() + ctx.channel() + " Receive remoteServer data: {} bytes => {}",
			byteBuf.readableBytes(), ByteBufUtil.hexDump(byteBuf));
		try (ByteArrayOutputStream _localOutStream = new ByteArrayOutputStream()) {
			if (relayChannel.isActive()) {
				if (!byteBuf.hasArray()) {
					int len = byteBuf.readableBytes();
					byte[] arr = new byte[len];
					byteBuf.getBytes(0, arr);
					if (isProxy) {
						crypt.decrypt(arr, arr.length, _localOutStream);
						arr = _localOutStream.toByteArray();
					}
					relayChannel.writeAndFlush(Unpooled.wrappedBuffer(arr));
					logger.info(Constants.LOG_MSG + ctx.channel() + " SendLocal message:isProxy = {},length = {}, channel = {}",
						isProxy, arr.length, relayChannel);
				}
			}
		} catch (Exception e) {
			logger.error(Constants.LOG_MSG + ctx.channel() + " Receive remoteServer data error: ", e);
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		if (relayChannel.isActive()) {
			SocksServerUtils.closeOnFlush(relayChannel);
			SocksServerUtils.closeOnFlush(ctx.channel());
		}
		logger.info(Constants.LOG_MSG + ctx.channel() + " InRelay channelInactive close");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		logger.error(Constants.LOG_MSG + ctx.channel(), cause);
		ctx.close();
	}
}
