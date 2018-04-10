package com.shadowsocks.client;

import com.shadowsocks.client.config.Server;
import com.shadowsocks.client.config.ServerConfig;
import com.shadowsocks.client.utils.PacFilter;
import com.shadowsocks.common.encryption.CryptFactory;
import com.shadowsocks.common.encryption.ICrypt;
import com.shadowsocks.common.utils.SocksServerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;

import static com.shadowsocks.common.config.Constants.LOG_MSG;

public final class ConnectHandler extends SimpleChannelInboundHandler<SocksMessage> {

	private static final Logger logger = LoggerFactory.getLogger(ConnectHandler.class);

	private final Bootstrap b = new Bootstrap();
	private boolean isProxy;

	/**
	 * 客户端 与 SocksClient 之间建立的 Channel 称为 clientChannel；
	 * SocksClient 与 目标地址 建立的，称为 OutboundChannel。
	 *
	 * @param ctx
	 * @param message
	 * @throws Exception
	 */
	@Override
	public void channelRead0(final ChannelHandlerContext ctx, final SocksMessage message) throws Exception {

		final Channel clientChannel = ctx.channel();

		final Server server = ServerConfig.getServer();
		if (server == null) {
			SocksServerUtils.closeOnFlush(ctx.channel());
			return;
		}

		final ICrypt crypt = CryptFactory.get(server.getMethod(), server.getPassword());

		// TODO：使用此 crypt 避免每次都重新生成？
		// final ICrypt crypt = server.getCrypt();

		clientChannel.attr(ServerConfig.CRYPT_KEY).set(crypt);

		if (message instanceof Socks5CommandRequest) { // 如果是socks4执行本方法
			logger.info(LOG_MSG + " Socks5 connection, connection channel={}", clientChannel);

			final Socks5CommandRequest request = (Socks5CommandRequest) message;
			isProxy = PacFilter.isProxy(request.dstAddr()); // 是否代理
			logger.info(LOG_MSG + " 是否使用代理：" + request.dstAddr() + " => " + isProxy);

			clientChannel.attr(ServerConfig.IS_PROXY).set(isProxy);
			clientChannel.attr(ServerConfig.DST_ADDR).set(request.dstAddr());

			Promise<Channel> promise = ctx.executor().newPromise();
			promise.addListener((FutureListener<Channel>) future -> { // Promise 继承了 Future，这里 future 即上文的 promise
				logger.debug(LOG_MSG + " future={}, promise={}", future, promise);

				final Channel remoteChannel = future.getNow();
				remoteChannel.attr(ServerConfig.CRYPT_KEY).set(crypt);
				remoteChannel.attr(ServerConfig.IS_PROXY).set(isProxy);
				remoteChannel.attr(ServerConfig.DST_ADDR).set(request.dstAddr());

				// 互相保留对方的channel，进行数据交互
				clientChannel.attr(ServerConfig.REMOTE_CHANNEL).set(remoteChannel);//
				remoteChannel.attr(ServerConfig.CLIENT_CHANNEL).set(clientChannel);// 绑定在clientChannel属性上

				if (future.isSuccess()) {
					logger.info(LOG_MSG + " Promise success => clientChannel={} 和 remoteChannel={}", clientChannel, remoteChannel);

					ChannelFuture responseFuture =
						clientChannel.writeAndFlush(new DefaultSocks5CommandResponse(
							Socks5CommandStatus.SUCCESS, request.dstAddrType(), request.dstAddr(), request.dstPort()));

					responseFuture.addListener((ChannelFutureListener) channelFuture -> {
						if (isProxy) {
							sendConnectRemoteMessage(crypt, request, remoteChannel);
							logger.debug(LOG_MSG + " Send remote connection success, ");
						}
						ctx.pipeline().remove(ConnectHandler.this);
						ctx.pipeline().addLast(new OutRelayHandler());
						remoteChannel.pipeline().addLast(new InRelayHandler());
					});
				} else {
					logger.info(LOG_MSG + " Promise failed => promise ctx channel={} 和 remoteChannel={}", clientChannel, remoteChannel);
					clientChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
					SocksServerUtils.closeOnFlush(clientChannel);
				}
			});

			b.group(clientChannel.eventLoop()) // 《Netty in Action》8.4节：从 Channel 引导客户端，从已经被接受的子 Channel 中引导一个客户端 Channel，在两个 Channel 之间共享 EventLoop
				.channel(NioSocketChannel.class)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
				.option(ChannelOption.SO_KEEPALIVE, true)
				.handler(new RemoteHandler(promise));

			// 连接目标服务器器
			String proxyHost;
			Integer proxyPort;
			if (isProxy) {
				proxyHost = server.getHost();
				proxyPort = Integer.valueOf(server.getPort());
			} else {
				proxyHost = request.dstAddr();
				proxyPort = request.dstPort();
			}

			b.connect(proxyHost, proxyPort)
				.addListener((ChannelFutureListener) future -> {

					if (future.isSuccess()) {
						logger.info(LOG_MSG + " Remote connection success => clientChannel={} 和 outboundChannel={}", clientChannel, promise.getNow());
					} else {
						logger.error(LOG_MSG + " Remote connection failed => clientChannel={} 和 outboundChannel={}", clientChannel, promise.getNow());
						clientChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
						SocksServerUtils.closeOnFlush(clientChannel); // 失败的话就关闭客户端和用户的连接
					}
				});
		} else {
			logger.error("socks protocol is not socks5");
			ctx.close();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error(LOG_MSG + ctx.channel(), cause);
		SocksServerUtils.closeOnFlush(ctx.channel());
	}

	/**
	 * localServer和remoteServer进行connect时，发送数据的方法以及格式（此格式拼接后需要加密之后才可发送）：
	 * <p>
	 * +------+----------+----------+
	 * | ATYP | DST.ADDR | DST.PORT |
	 * +------+----------+----------+
	 * | 1    | Variable | 2        |
	 * +------+----------+----------+
	 * <p>
	 * 其中变长DST.ADDR：
	 * - 4 bytes for IPv4 address
	 * - 1 byte of name length followed by 1–255 bytes the domain name
	 * - 16 bytes for IPv6 address
	 *
	 * @param request
	 * @param outboundChannel
	 */
	private void sendConnectRemoteMessage(ICrypt crypt, Socks5CommandRequest request, Channel outboundChannel) throws Exception {
		String dstAddr = request.dstAddr();
		ByteBuf buf = Unpooled.buffer();
		if (Socks5CommandType.CONNECT == request.type()) { // ATYP 1 byte
			buf.writeByte(request.dstAddrType().byteValue());
			if (Socks5AddressType.IPv4 == request.dstAddrType()) { // // DST.ADDR: 4 bytes
				InetAddress inetAddress = InetAddress.getByName(dstAddr);
				buf.writeBytes(inetAddress.getAddress());
			} else if (Socks5AddressType.DOMAIN == request.dstAddrType()) { // DST.ADDR: 1 + N bytes
				byte[] dstAddrBytes = dstAddr.getBytes(); // DST.ADDR: 变长
				buf.writeByte(dstAddrBytes.length); // DST.ADDR len: 1 byte
				buf.writeBytes(dstAddrBytes);      // DST.ADDR content: N bytes
			} else if (Socks5AddressType.IPv6 == request.dstAddrType()) { // DST.ADDR: 16 bytes
				InetAddress inetAddress = InetAddress.getByName(dstAddr);
				buf.writeBytes(inetAddress.getAddress());
			}
			buf.writeShort(request.dstPort()); // DST.PORT
		} else {
			logger.error(LOG_MSG + "Connect type error, requst={}, get={}", Socks5CommandType.CONNECT, request.type());
		}

		byte[] data = ByteBufUtil.getBytes(buf);
		ByteArrayOutputStream _remoteOutStream = new ByteArrayOutputStream();
		crypt.encrypt(data, data.length, _remoteOutStream);
		data = _remoteOutStream.toByteArray();
		outboundChannel.writeAndFlush(Unpooled.wrappedBuffer(data)); // 发送数据
	}

}
