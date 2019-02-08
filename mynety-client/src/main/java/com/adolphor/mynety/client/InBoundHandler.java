package com.adolphor.mynety.client;

import com.adolphor.mynety.client.config.ClientConfig;
import com.adolphor.mynety.client.config.Server;
import com.adolphor.mynety.client.utils.PacFilter;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.encryption.CryptFactory;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.utils.ChannelUtils;
import com.adolphor.mynety.common.wrapper.AbstractInBoundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static com.adolphor.mynety.common.constants.Constants.ATTR_CRYPT_KEY;
import static com.adolphor.mynety.common.constants.Constants.ATTR_IN_RELAY_CHANNEL;
import static com.adolphor.mynety.common.constants.Constants.ATTR_IS_PROXY;
import static com.adolphor.mynety.common.constants.Constants.ATTR_OUT_RELAY_CHANNEL_REF;
import static com.adolphor.mynety.common.constants.Constants.ATTR_SOCKS5_REQUEST;
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
@ChannelHandler.Sharable
public final class InBoundHandler extends AbstractInBoundHandler<ByteBuf> {

  public static final InBoundHandler INSTANCE = new InBoundHandler();

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);

    final Server server = ClientConfig.getAvailableServer();
    if (server == null) {
      ChannelUtils.closeOnFlush(ctx.channel());
      return;
    }
    // 获取处理此channel的加解密对象
    ICrypt crypt = CryptFactory.get(server.getMethod(), server.getPassword());
    ctx.channel().attr(ATTR_CRYPT_KEY).set(crypt);

    Socks5CommandRequest socks5CmdRequest = ctx.channel().attr(ATTR_SOCKS5_REQUEST).get();
    String dstAddr = socks5CmdRequest.dstAddr();
    Integer dstPort = socks5CmdRequest.dstPort();
    boolean isDeny = PacFilter.isDeny(dstAddr);
    if (isDeny) {
      logger.warn("[ {}{} ]【socks客户端激活】拦截请求: {}:{}", ctx.channel().id(), Constants.LOG_MSG, dstAddr, dstPort);
      channelClose(ctx);
      return;
    }
    Boolean isProxy = PacFilter.isProxy(dstAddr);
    ctx.channel().attr(ATTR_IS_PROXY).set(isProxy);

    Bootstrap outBoundBootStrap = new Bootstrap();
    outBoundBootStrap.group(ctx.channel().eventLoop())
        .channel(Constants.channelClass)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)
        .handler(OutBoundInitializer.INSTANCE);

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
      ChannelFuture channelFuture = outBoundBootStrap.connect(connHost, connPort);
      channelFuture.addListener((ChannelFutureListener) future -> {
        if (future.isSuccess()) {
          // 远程连接实例化
          Channel outRelayChannel = future.channel();
          logger.debug("[ {}{}{} ]【{}】建立socks远程连接，连接信息: {}:{}", ctx.channel().id(), Constants.LOG_MSG, outRelayChannel.id(), getSimpleName(this), connHost, connPort);
          ctx.channel().attr(ATTR_OUT_RELAY_CHANNEL_REF).get().set(outRelayChannel);
          outRelayChannel.attr(ATTR_IN_RELAY_CHANNEL).set(ctx.channel());

          // 如果使用了代理，那么就要发送远程连接指令，且需要在收到socks连接成功返回信息之后，再告诉客户端连接成功
          if (isProxy) {
            sendConnectRemoteMessage(ctx.channel(), outRelayChannel, crypt, socks5CmdRequest);
          }
          // 如果没使用代理，那么直接告诉客户端连接成功
          else {
            DefaultSocks5CommandResponse socks5cmdResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, socks5CmdRequest.dstAddrType(), dstAddr, socks5CmdRequest.dstPort());
            ctx.channel().writeAndFlush(socks5cmdResponse);
            logger.debug("[ {}{}{} ]【{}】建立socks远程连接，告诉客户端socks连接请求成功: {}", ctx.channel().id(), Constants.LOG_MSG_IN, outRelayChannel.id(), getSimpleName(this), socks5cmdResponse);
          }
          logger.debug("[ {}{}{} ]【{}】建立socks远程连接，{} 连接成功: {}:{}", ctx.channel().id(), Constants.LOG_MSG, outRelayChannel.id(), getSimpleName(this), isProxy ? "远程代理" : "请求地址", connHost, connPort);
        } else {
          logger.warn("[ {}{} ]【{}】建立socks远程连接，{} 连接失败: {}:{}", ctx.channel().id(), Constants.LOG_MSG, isProxy ? "远程代理" : "请求地址", getSimpleName(this), connHost, connPort);
          logger.warn(ctx.channel().toString(), future.cause());
          future.cancel(true);
          channelClose(ctx);
        }
      });

    } catch (Exception e) {
      logger.error(ctx.channel().id() + LOG_MSG_OUT + " Send data to remoteServer error: ", e);
      channelClose(ctx);
    }
  }

  /**
   * 客户端 与 SocksClient 之间建立的 Channel 称为 inRelayChannel；
   * SocksClient 与 目标地址 建立的，称为 OutboundChannel。
   *
   * @param ctx
   * @param msg
   * @throws Exception
   */
  @Override
  public void channelRead0(final ChannelHandlerContext ctx, final ByteBuf msg) throws Exception {
    AtomicReference<Channel> outRelayChannelRef = ctx.channel().attr(ATTR_OUT_RELAY_CHANNEL_REF).get();
    Boolean isProxy = ctx.channel().attr(ATTR_IS_PROXY).get();
    ICrypt crypt = ctx.channel().attr(ATTR_CRYPT_KEY).get();

    logger.debug("[ {}{}{} ]【{}】socks收到客户请求消息，收到消息: {} bytes => {}", ctx.channel().id(), Constants.LOG_MSG, (outRelayChannelRef.get() != null) ? outRelayChannelRef.get().id() : "", getSimpleName(this), msg.readableBytes(), msg);
    if (msg.readableBytes() <= 0) {
      return;
    }

    if (!msg.hasArray()) {
      int len = msg.readableBytes();
      byte[] temp = new byte[len];
      msg.getBytes(0, temp);
      if (isProxy) {
        ByteArrayOutputStream _remoteOutStream = new ByteArrayOutputStream();
        crypt.encrypt(temp, temp.length, _remoteOutStream);
        temp = _remoteOutStream.toByteArray();
        logger.debug("[ {}{}{} ]【{}】socks收到客户请求消息，消息需要加密：{} bytes => {}", ctx.channel().id(), Constants.LOG_MSG, (outRelayChannelRef.get() != null) ? outRelayChannelRef.get().id() : "", getSimpleName(this), msg.readableBytes(), msg);
      }
      ByteBuf requestBuf = Unpooled.wrappedBuffer(temp);
      outRelayChannelRef.get().writeAndFlush(requestBuf);
      logger.debug("[ {}{}{} ]【{}】socks收到客户请求消息，发送消息到 {} : {} bytes => {}", ctx.channel().id(), LOG_MSG_OUT, outRelayChannelRef.get().id(), getSimpleName(this), isProxy ? "远程代理" : "请求地址", msg.readableBytes(), msg);
    } else {
      logger.warn("[ {}{}{} ]【{}】socks收到客户请求消息，不支持的消息类型: {}", ctx.channel().id(), Constants.LOG_MSG, outRelayChannelRef.get().id(), getSimpleName(this), msg);
    }
  }

  /**
   * localServer 和 proxyServer 进行connect时，发送数据的方法以及格式（此格式拼接后需要加密之后才可发送）：
   * <p>
   * +------+----------+---------------+
   * | ATYP | DST.ADDR | DST.ATTR_PORT |
   * +------+----------+---------------+
   * | 1    | Variable | 2             |
   * +------+----------+---------------+
   * <p>
   * 其中变长DST.ADDR：
   * - 4 bytes for IPv4 address
   * - 1 byte of name length followed by 1–255 bytes the domain name
   * - 16 bytes for IPv6 address
   *
   * @param inRelayChannel  本地连接
   * @param outRelayChannel 远程代理连接
   */
  private void sendConnectRemoteMessage(Channel inRelayChannel, Channel outRelayChannel, ICrypt crypt, Socks5CommandRequest socks5CmdRequest) throws Exception {

    Socks5CommandRequest socks5Request = inRelayChannel.attr(ATTR_SOCKS5_REQUEST).get();
    Socks5AddressType dstAddrType = socks5Request.dstAddrType();
    String dstAddr = socks5Request.dstAddr();
    int dstPort = socks5Request.dstPort();
    logger.debug("[ {}{}{} ]【初始化socks远程连接】目的请求地址信息: type={} => {}:{}", inRelayChannel.id(), Constants.LOG_MSG, outRelayChannel.id(), dstAddrType, dstAddr, dstPort);

    ByteBuf srcBuf = Unpooled.buffer();
    // ATYP 1 byte
    srcBuf.writeByte(socks5Request.dstAddrType().byteValue());
    // DST.ADDR: 4 bytes
    if (Socks5AddressType.IPv4 == dstAddrType) {
      InetAddress inetAddress = InetAddress.getByName(dstAddr);
      srcBuf.writeBytes(inetAddress.getAddress());
    }
    // DST.ADDR: 16 bytes
    else if (Socks5AddressType.IPv6 == dstAddrType) {
      InetAddress inetAddress = InetAddress.getByName(dstAddr);
      srcBuf.writeBytes(inetAddress.getAddress());
    }
    // DST.ADDR: 1 + N bytes
    else if (Socks5AddressType.DOMAIN == dstAddrType) {
      byte[] dstAddrBytes = dstAddr.getBytes(StandardCharsets.UTF_8);
      srcBuf.writeByte(dstAddrBytes.length);
      srcBuf.writeBytes(dstAddrBytes);
    } else {
      logger.debug("[ {}{}{} ]【初始化socks远程连接】链接出错，错误的地址类型：{} ", inRelayChannel.id(), Constants.LOG_MSG, outRelayChannel.id(), dstAddrType);
    }
    // DST.ATTR_PORT
    srcBuf.writeShort(dstPort);

    logger.debug("[ {}{}{} ]【初始化socks远程连接】未加密的请求连接内容: {} bytes => {}", inRelayChannel.id(), Constants.LOG_MSG, outRelayChannel.id(), srcBuf.readableBytes(), srcBuf);
    byte[] temp = ByteBufUtil.getBytes(srcBuf);
    ByteArrayOutputStream _remoteOutStream = new ByteArrayOutputStream();
    crypt.encrypt(temp, temp.length, _remoteOutStream);
    temp = _remoteOutStream.toByteArray();
    ByteBuf encryptedBuf = Unpooled.wrappedBuffer(temp);
    logger.debug("[ {}{}{} ]【初始化socks远程连接】发送加密后的请求连接内容: {} bytes => {}", inRelayChannel.id(), LOG_MSG_OUT, outRelayChannel.id(), temp.length, encryptedBuf);
    // 发送数据
    outRelayChannel.writeAndFlush(encryptedBuf).addListener((ChannelFutureListener) future -> {
      // 告诉客户端连接成功
      DefaultSocks5CommandResponse socks5cmdResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, socks5CmdRequest.dstAddrType(), dstAddr, socks5CmdRequest.dstPort());
      inRelayChannel.writeAndFlush(socks5cmdResponse);
      logger.debug("[ {}{}{} ]【建立socks远程连接】告诉客户端socks连接请求成功: {}", inRelayChannel.id(), Constants.LOG_MSG_IN, outRelayChannel.id(), socks5cmdResponse);

    });
  }

}
