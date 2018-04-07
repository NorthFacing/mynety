package com.shadowsocks.server;

import com.shadowsocks.common.encryption.CryptFactory;
import com.shadowsocks.common.encryption.CryptUtil;
import com.shadowsocks.common.encryption.ICrypt;
import com.shadowsocks.server.Config.ServerConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

public class SocksServerHandler extends SimpleChannelInboundHandler {

	public static final Logger logger = LoggerFactory.getLogger(SocksServerHandler.class);

	public static final SocksServerHandler INSTANCE = new SocksServerHandler();

	private ICrypt crypt;

	public SocksServerHandler() {
		this.crypt = CryptFactory.get(ServerConfig.METHOD, ServerConfig.PASSWORD);
	}

	/**
	 * +------+----------+----------+
	 * | ATYP | DST.ADDR | DST.PORT |
	 * +------+----------+----------+
	 * | 1    | Variable | 2        |
	 * +------+----------+----------+
	 *
	 * @param ctx
	 * @param msg
	 * @throws Exception
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf buff = (ByteBuf) msg;
		if (buff.readableBytes() <= 0) {
			return;
		}
		ByteBuf dataBuff = Unpooled.buffer();
		dataBuff.writeBytes(CryptUtil.decrypt(crypt, msg));
		if (dataBuff.readableBytes() < 4) {
			return;
		}
		String host = null;
		int port = 0;
		int addressType = dataBuff.getUnsignedByte(0);
		if (addressType == Socks5AddressType.IPv4.byteValue()) {
			if (dataBuff.readableBytes() < 6) {
				return;
			}
			ByteBuf ipBytes = dataBuff.readBytes(4);
			host = InetAddress.getByAddress(ipBytes.array()).toString().substring(1);
			port = dataBuff.readShort();
		} else if (addressType == Socks5AddressType.DOMAIN.byteValue()) {
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
		} else if (addressType == Socks5AddressType.IPv4.byteValue()) {
			if (dataBuff.readableBytes() < 18) {
				return;
			}
			ByteBuf ipBytes = dataBuff.readBytes(16);
			host = InetAddress.getByAddress(ipBytes.array()).toString().substring(1);
			port = dataBuff.readShort();
		} else {
			throw new IllegalStateException("unknown address type: " + addressType);
		}
		logger.debug("addressType = " + addressType + ",host = " + host + ",port = " + port + ",dataBuff = "
			+ dataBuff.readableBytes());
//		ctx.channel().pipeline().addLast(new ClientProxyHandler(host, port, ctx, dataBuff, _crypt));
//		ctx.channel().pipeline().remove(this);
	}

}
