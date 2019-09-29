package com.adolphor.mynety.client.socks5;

import com.adolphor.mynety.client.config.Server;
import com.adolphor.mynety.client.utils.NetUtils;
import com.adolphor.mynety.client.utils.PacFilter;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.encryption.CryptFactory;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.wrapper.AbstractInBoundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5Message;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.util.concurrent.atomic.AtomicReference;

import static com.adolphor.mynety.common.constants.Constants.ATTR_CRYPT_KEY;
import static com.adolphor.mynety.common.constants.Constants.ATTR_IN_RELAY_CHANNEL;
import static com.adolphor.mynety.common.constants.Constants.ATTR_IS_PROXY;
import static com.adolphor.mynety.common.constants.Constants.ATTR_OUT_RELAY_CHANNEL_REF;
import static com.adolphor.mynety.common.constants.Constants.ATTR_SOCKS5_REQUEST;
import static com.adolphor.mynety.common.constants.Constants.CONNECT_TIMEOUT;
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
    // check proxy server
    final Server server = NetUtils.getBestServer();
    if (server == null) {
      channelClose(ctx);
      return;
    }
    //check crypt cipher instance
    ICrypt crypt;
    try {
      crypt = CryptFactory.get(server.getMethod(), server.getPassword());
    } catch (InvalidAlgorithmParameterException e) {
      logger.error(e.getMessage(), e);
      channelClose(ctx);
      return;
    }
    ctx.channel().attr(ATTR_CRYPT_KEY).set(crypt);

    Socks5CommandRequest socksRequest = ctx.channel().attr(ATTR_SOCKS5_REQUEST).get();
    String dstAddr = socksRequest.dstAddr();
    Integer dstPort = socksRequest.dstPort();
    boolean isDeny = PacFilter.isDeny(dstAddr);
    if (isDeny) {
      logger.info("[ {}{} ] deny request by pac rules => {}:{}", ctx.channel().id(), Constants.LOG_MSG, dstAddr, dstPort);
      channelClose(ctx);
      return;
    }
    Boolean isProxy = PacFilter.isProxy(dstAddr);
    ctx.channel().attr(ATTR_IS_PROXY).set(isProxy);

    Bootstrap outBoundBootStrap = new Bootstrap();
    outBoundBootStrap.group(ctx.channel().eventLoop())
        .channel(Constants.channelClass)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT)
        .handler(OutBoundInitializer.INSTANCE);

    String connHost;
    Integer connPort;
    if (isProxy) {
      connHost = server.getHost();
      connPort = Integer.valueOf(server.getPort());
    } else {
      connHost = dstAddr;
      connPort = dstPort;
    }

    outBoundBootStrap.connect(connHost, connPort).addListener((ChannelFutureListener) future -> {
      if (future.isSuccess()) {
        Channel outRelayChannel = future.channel();
        ctx.channel().attr(ATTR_OUT_RELAY_CHANNEL_REF).get().set(outRelayChannel);
        outRelayChannel.attr(ATTR_IN_RELAY_CHANNEL).set(ctx.channel());
        if (isProxy) {
          sendConnectRemoteMessage(ctx.channel(), outRelayChannel, crypt, socksRequest);
        } else {
          Socks5Message socks5cmdResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS,
              socksRequest.dstAddrType(), dstAddr, socksRequest.dstPort());
          ctx.channel().writeAndFlush(socks5cmdResponse);
        }
      } else {
        throw new Exception(future.cause());
      }
    });
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
  public void channelRead0(final ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    if (msg.readableBytes() <= 0) {
      return;
    }

    AtomicReference<Channel> outRelayChannelRef = ctx.channel().attr(ATTR_OUT_RELAY_CHANNEL_REF).get();
    if (ctx.channel().attr(ATTR_IS_PROXY).get()) {
      ByteBuf encryptBuf = ctx.channel().attr(ATTR_CRYPT_KEY).get().encrypt(msg);
      outRelayChannelRef.get().writeAndFlush(encryptBuf);
    } else {
      outRelayChannelRef.get().writeAndFlush(msg);
    }

  }

  /**
   * to localServer & proxyServer, before send msg should be encrypt:
   * <p>
   * +------+----------+---------------+
   * | ATYP | DST.ADDR | DST.ATTR_PORT |
   * +------+----------+---------------+
   * | 1    | Variable | 2             |
   * +------+----------+---------------+
   * <p>
   * dynamic DST.ADDR：
   * - 4 bytes for IPv4 address
   * - 1 byte of name length followed by 1–255 bytes the domain name
   * - 16 bytes for IPv6 address
   *
   * @param inRelayChannel
   * @param outRelayChannel
   */
  private void sendConnectRemoteMessage(Channel inRelayChannel, Channel outRelayChannel, ICrypt crypt, Socks5CommandRequest socks5CmdRequest) throws Exception {

    Socks5CommandRequest socks5Request = inRelayChannel.attr(ATTR_SOCKS5_REQUEST).get();
    Socks5AddressType dstAddrType = socks5Request.dstAddrType();
    String dstAddr = socks5Request.dstAddr();
    int dstPort = socks5Request.dstPort();

    ByteBuf connBuf = Unpooled.directBuffer();
    // ATYP 1 byte
    connBuf.writeByte(socks5Request.dstAddrType().byteValue());
    // DST.ADDR: 4 bytes
    if (Socks5AddressType.IPv4 == dstAddrType) {
      InetAddress inetAddress = InetAddress.getByName(dstAddr);
      connBuf.writeBytes(inetAddress.getAddress());
    }
    // DST.ADDR: 16 bytes
    else if (Socks5AddressType.IPv6 == dstAddrType) {
      InetAddress inetAddress = InetAddress.getByName(dstAddr);
      connBuf.writeBytes(inetAddress.getAddress());
    }
    // DST.ADDR: 1 + N bytes
    else if (Socks5AddressType.DOMAIN == dstAddrType) {
      byte[] dstAddrBytes = dstAddr.getBytes(StandardCharsets.UTF_8);
      connBuf.writeByte(dstAddrBytes.length);
      connBuf.writeBytes(dstAddrBytes);
    } else {
      logger.error("[ {}{}{} ] {} wrong request address type: {}", inRelayChannel.id(), Constants.LOG_MSG, outRelayChannel.id(), getSimpleName(this), dstAddrType);
      throw new IllegalArgumentException("wrong request address type:" + dstAddrType);
    }
    // DST.ATTR_PORT
    connBuf.writeShort(dstPort);
    ByteBuf encryptBuf = crypt.encrypt(connBuf);
    connBuf.release();
    outRelayChannel.writeAndFlush(encryptBuf).addListener((ChannelFutureListener) future -> {
      Socks5Message socks5cmdResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, socks5CmdRequest.dstAddrType(), dstAddr, socks5CmdRequest.dstPort());
      inRelayChannel.writeAndFlush(socks5cmdResponse);
    });
  }

}
