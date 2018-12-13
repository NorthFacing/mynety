package com.adolphor.mynety.client;

import com.adolphor.mynety.client.config.ClientConfig;
import com.adolphor.mynety.client.config.Server;
import com.adolphor.mynety.client.utils.PacFilter;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.encryption.CryptFactory;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.wrapper.AbstractInRelayHandler;
import com.adolphor.mynety.common.utils.SocksServerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;

import static com.adolphor.mynety.common.constants.Constants.LOG_MSG_OUT;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * 连接处理器：建立本地和远程服务器（代理服务器或者是目的服务器，根据代理规则来确定）的连接，
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
public final class ConnectionHandler extends AbstractInRelayHandler<ByteBuf> {

  private boolean isProxy;

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.info("[ {}{}{} ] {} channel active...", ctx.channel(), Constants.LOG_MSG, remoteChannelRef.get(), getSimpleName(this));

    Channel clientChannel = ctx.channel();
    clientChannel.attr(Constants.ATTR_REQUEST_TEMP_LIST).set(requestTempLists);

    final Server server = ClientConfig.getAvailableServer();
    if (server == null) {
      SocksServerUtils.closeOnFlush(ctx.channel());
      return;
    }
    final ICrypt crypt = CryptFactory.get(server.getMethod(), server.getPassword());
    ctx.channel().attr(Constants.ATTR_CRYPT_KEY).set(crypt);

    Socks5CommandRequest socks5CmdRequest = ctx.channel().attr(Constants.ATTR_SOCKS5_REQUEST).get();
    String dstAddr = socks5CmdRequest.dstAddr();
    Integer dstPort = socks5CmdRequest.dstPort();
    boolean isDeny = PacFilter.isDeny(dstAddr);
    if (isDeny) {
      logger.warn("[ {}{} ]  dst address is configured to be shutdown: {}:{}", ctx.channel(), Constants.LOG_MSG, dstAddr, dstPort);
      channelClose(ctx);
      return;
    }
    // TODO：使用此 crypt 避免每次都重新生成？
    // final ICrypt crypt = server.getCrypt();
    isProxy = PacFilter.isProxy(dstAddr); // 是否代理

    Bootstrap remoteBootStrap = new Bootstrap();
    remoteBootStrap.group(clientChannel.eventLoop())
        .channel(Constants.channelClass)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast(new RemoteHandler(ctx.channel(), isProxy, crypt));
            logger.info("[ {}{}{} ] out pipeline add handler: RemoteHandler", ctx.channel(), Constants.LOG_MSG, ch);
          }
        });

    // 连接目标服务器器
    String connHost;
    Integer connPort;
    if (isProxy) {
      connHost = server.getHost();
      connPort = Integer.valueOf(server.getPort());
    } else {
      connHost = dstAddr;
      connPort = dstPort;
    }

    try {
      ChannelFuture channelFuture = remoteBootStrap.connect(connHost, connPort);
      channelFuture.addListener((ChannelFutureListener) future -> {
        if (future.isSuccess()) {
          logger.debug("[ {}{}{} ] socks client connect to dst: {}:{}", clientChannel, Constants.LOG_MSG, future.channel(), connHost, connPort);
          Channel remoteChannel = future.channel();
          remoteChannelRef.set(remoteChannel);// 远程连接实例化
          if (isProxy) { // 如果使用了代理，那么就要发送远程连接指令
            sendConnectRemoteMessage(clientChannel, remoteChannel, crypt);
          }
          logger.debug("[ {}{}{} ] socks client connect {} success: {}:{}", clientChannel, Constants.LOG_MSG, future.channel(), isProxy ? "socks server" : "dst host", connHost, connPort);
          DefaultSocks5CommandResponse socks5cmdResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, socks5CmdRequest.dstAddrType(), dstAddr, socks5CmdRequest.dstPort());
          clientChannel.writeAndFlush(socks5cmdResponse);  // 告诉客户端连接成功
          logger.debug("[ {}{}{} ] socks client connection success response to user-agent: {}", clientChannel, Constants.LOG_MSG_IN, future.channel(), socks5cmdResponse);
        } else {
          logger.debug("[ {}{}{} ] socks client connect {} failed: {}:{}", clientChannel, Constants.LOG_MSG, future.channel(), isProxy ? "socks server" : "dst host", connHost, connPort);
          future.cancel(true);
          channelClose(ctx);
        }
      });

    } catch (Exception e) {
      logger.error("socks5 connect internet error", e);
      channelClose(ctx);
    }
  }

  /**
   * 客户端 与 SocksClient 之间建立的 Channel 称为 clientChannel；
   * SocksClient 与 目标地址 建立的，称为 OutboundChannel。
   *
   * @param ctx
   * @param msg
   * @throws Exception
   */
  @Override
  public void channelRead0(final ChannelHandlerContext ctx, final ByteBuf msg) throws Exception {
    Channel remoteChannel = remoteChannelRef.get();
    logger.debug("[ {}{}{} ] [ConnectionHandler-channelRead0] socks client connection handler channelRead: {} bytes => {}", ctx.channel(), Constants.LOG_MSG, remoteChannel, msg.readableBytes(), msg);
    if (msg.readableBytes() <= 0) {
      return;
    }

    try (ByteArrayOutputStream _remoteOutStream = new ByteArrayOutputStream()) {
      if (!msg.hasArray()) {
        int len = msg.readableBytes();
        byte[] temp = new byte[len];
        msg.getBytes(0, temp);
        if (isProxy) {
          logger.debug("[ {}{}{} ] [ConnectionHandler-channelRead0] msg need to encrypt...", ctx.channel(), Constants.LOG_MSG, remoteChannel);
          ICrypt crypt = ctx.channel().attr(Constants.ATTR_CRYPT_KEY).get();
          crypt.encrypt(temp, temp.length, _remoteOutStream);
          temp = _remoteOutStream.toByteArray();
        }
        ByteBuf encryptedBuf = Unpooled.wrappedBuffer(temp);
        synchronized (requestTempLists) {
          if (remoteChannel != null) {
            remoteChannel.writeAndFlush(encryptedBuf);
            logger.debug("[ {}{}{} ] [ConnectionHandler-channelRead0] write msg to {} channel: {} bytes => {}",
                ctx.channel(), LOG_MSG_OUT, remoteChannelRef.get(), isProxy ? "socks server" : "dst host", msg.readableBytes(), msg);
          } else {
            requestTempLists.add(encryptedBuf);
            logger.debug("[ {}{}{} ] [ConnectionHandler-channelRead0] add msg to temp list: {}", ctx.channel(), Constants.LOG_MSG, remoteChannel, msg);
          }
        }
      } else {
        logger.warn("[ {}{}{} ] [ConnectionHandler-channelRead0] socks client unhandled msg: {}", ctx.channel(), Constants.LOG_MSG, remoteChannel, msg);
      }
    } catch (Exception e) {
      logger.error(ctx.channel() + LOG_MSG_OUT + remoteChannel + " Send data to remoteServer error: ", e);
      channelClose(ctx);
    }
  }

  /**
   * localServer 和 proxyServer 进行connect时，发送数据的方法以及格式（此格式拼接后需要加密之后才可发送）：
   * <p>
   * +------+----------+----------+
   * | ATYP | DST.ADDR | DST.ATTR_PORT |
   * +------+----------+----------+
   * | 1    | Variable | 2        |
   * +------+----------+----------+
   * <p>
   * 其中变长DST.ADDR：
   * - 4 bytes for IPv4 address
   * - 1 byte of name length followed by 1–255 bytes the domain name
   * - 16 bytes for IPv6 address
   *
   * @param clientChannel 本地连接
   * @param remoteChannel 远程代理连接
   * @param crypt         加密
   */
  private void sendConnectRemoteMessage(Channel clientChannel, Channel remoteChannel, ICrypt crypt) throws Exception {

    Socks5CommandRequest socks5Request = clientChannel.attr(Constants.ATTR_SOCKS5_REQUEST).get();
    Socks5AddressType dstAddrType = socks5Request.dstAddrType();
    String dstAddr = socks5Request.dstAddr();
    int dstPort = socks5Request.dstPort();
    logger.debug("[ {}{}{} ] dst host for connecting: type={} => {}:{}", clientChannel, Constants.LOG_MSG, remoteChannel, dstAddrType, dstAddr, dstPort);

    ByteBuf srcBuf = Unpooled.buffer();
    srcBuf.writeByte(socks5Request.dstAddrType().byteValue()); // ATYP 1 byte
    if (Socks5AddressType.IPv4 == dstAddrType) { // // DST.ADDR: 4 bytes
      InetAddress inetAddress = InetAddress.getByName(dstAddr);
      srcBuf.writeBytes(inetAddress.getAddress());
    } else if (Socks5AddressType.IPv6 == dstAddrType) { // DST.ADDR: 16 bytes
      InetAddress inetAddress = InetAddress.getByName(dstAddr);
      srcBuf.writeBytes(inetAddress.getAddress());
    } else if (Socks5AddressType.DOMAIN == dstAddrType) { // DST.ADDR: 1 + N bytes
      byte[] dstAddrBytes = dstAddr.getBytes(); // DST.ADDR: 变长
      srcBuf.writeByte(dstAddrBytes.length); // DST.ADDR len: 1 byte
      srcBuf.writeBytes(dstAddrBytes);      // DST.ADDR content: N bytes
    } else {
      logger.debug("[ {}{}{} ] socks client connect msg error: wrong address type => {} ", clientChannel, Constants.LOG_MSG, remoteChannel, dstAddrType);
    }
    srcBuf.writeShort(dstPort); // DST.ATTR_PORT

    logger.debug("[ {}{}{} ] socks client connect server source msg: {} bytes => {}", clientChannel, Constants.LOG_MSG, remoteChannel, srcBuf.readableBytes(), srcBuf);
    byte[] temp = ByteBufUtil.getBytes(srcBuf);
    ByteArrayOutputStream _remoteOutStream = new ByteArrayOutputStream();
    logger.debug("[ {}{}{} ] connect msg need to encrypt...", clientChannel, Constants.LOG_MSG, remoteChannel);
    crypt.encrypt(temp, temp.length, _remoteOutStream);
    temp = _remoteOutStream.toByteArray();
    ByteBuf encryptedBuf = Unpooled.wrappedBuffer(temp);
    remoteChannel.writeAndFlush(encryptedBuf); // 发送数据
    logger.debug("[ {}{}{} ] socks client send decrypted msg to server: {} bytes => {}", clientChannel, LOG_MSG_OUT, remoteChannel, temp.length, encryptedBuf);
  }

}
