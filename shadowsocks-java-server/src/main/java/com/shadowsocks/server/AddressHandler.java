package com.shadowsocks.server;

import com.shadowsocks.common.encryption.CryptFactory;
import com.shadowsocks.common.encryption.CryptUtil;
import com.shadowsocks.common.encryption.ICrypt;
import com.shadowsocks.server.Config.Config;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socks.SocksAddressType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

@ChannelHandler.Sharable
public class AddressHandler extends SimpleChannelInboundHandler {

	private static Logger logger = LoggerFactory.getLogger(AddressHandler.class);

	public static final AddressHandler INSTANCE = new AddressHandler();

	public AddressHandler() {
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.channel().attr(Config.CRYPT_KEY).setIfAbsent(CryptFactory.get(Config.METHOD, Config.PASSWORD));
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
		logger.error("AddressHandler error", cause);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ctx.close();
		logger.info("AddressHandler channelInactive close");
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		ICrypt crypt = ctx.channel().attr(Config.CRYPT_KEY).get();

		ByteBuf buff = (ByteBuf) msg;

		if (buff.readableBytes() <= 0) {
			return;
		}
		ByteBuf dataBuff = Unpooled.buffer();
		dataBuff.writeBytes(CryptUtil.decrypt(crypt, msg));
		if (dataBuff.readableBytes() < 2) {
			return;
		}
		String host = null;
		int port = 0;
		int addressType = dataBuff.getUnsignedByte(0);
		if (addressType == SocksAddressType.IPv4.byteValue()) {
			if (dataBuff.readableBytes() < 7) {
				return;
			}
			dataBuff.readUnsignedByte();
			byte[] ipBytes = new byte[4];
			dataBuff.readBytes(ipBytes);
			host = InetAddress.getByAddress(ipBytes).toString().substring(1);
			port = dataBuff.readShort();
		} else if (addressType == SocksAddressType.DOMAIN.byteValue()) {
			int hostLength = dataBuff.getUnsignedByte(1);
			if (dataBuff.readableBytes() < hostLength + 4) {
				return;
			}
			dataBuff.readUnsignedByte();
			dataBuff.readUnsignedByte();
			byte[] hostBytes = new byte[hostLength];
			dataBuff.readBytes(hostBytes);
			host = new String(hostBytes);
			port = dataBuff.readShort();
		} else {
			throw new IllegalStateException("unknown address type: " + addressType);
		}
		logger.debug("addressType = " + addressType + ",host = " + host + ",port = " + port);
		ctx.channel().attr(Config.HOST).set(host);
		ctx.channel().attr(Config.PORT).set(port);
		ctx.channel().attr(Config.BUF).set(dataBuff);

		ctx.channel().pipeline().remove(this);
		ctx.channel().pipeline().addLast(new ConnectionHandler());
		ctx.pipeline().fireChannelActive();
	}
}