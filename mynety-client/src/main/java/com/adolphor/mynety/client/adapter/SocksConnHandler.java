package com.adolphor.mynety.client.adapter;

import com.adolphor.mynety.client.http.HttpOutBoundInitializer;
import com.adolphor.mynety.common.bean.Address;
import com.adolphor.mynety.common.wrapper.AbstractSimpleHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.socks.SocksAddressType;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import static com.adolphor.mynety.common.constants.Constants.ATTR_IN_RELAY_CHANNEL;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_ADDRESS;
import static com.adolphor.mynety.common.constants.Constants.IPV4_PATTERN;
import static com.adolphor.mynety.common.constants.Constants.IPV6_PATTERN;
import static com.adolphor.mynety.common.constants.Constants.RESERVED_BYTE;
import static com.adolphor.mynety.common.constants.HandlerName.socksConnHandler;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.4
 */
@Slf4j
@ChannelHandler.Sharable
public class SocksConnHandler extends AbstractSimpleHandler<ByteBuf> {

  public static final SocksConnHandler INSTANCE = new SocksConnHandler();

  /**
   * msg format:
   * +----+-----+-------+------+----------+---------------+
   * |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.ATTR_PORT |
   * +----+-----+-------+------+----------+---------------+
   * | 1  |  1  | X'00' |  1   | Variable |      2        |
   * +----+-----+-------+------+----------+---------------+
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
    Channel inRelayChannel = ctx.channel().attr(ATTR_IN_RELAY_CHANNEL).get();
    Address address = inRelayChannel.attr(ATTR_REQUEST_ADDRESS).get();
    final ByteBuf buf = Unpooled.buffer();
    buf.writeByte(SocksVersion.SOCKS5.byteValue());
    buf.writeByte(Socks5CommandType.CONNECT.byteValue());
    buf.writeByte(RESERVED_BYTE);
    String host = address.getHost();
    // IPv4：4 bytes for IPv4 address
    if (IPV4_PATTERN.matcher(host).find()) {
      buf.writeByte(SocksAddressType.IPv4.byteValue());
      InetAddress inetAddress = InetAddress.getByName(host);
      buf.writeBytes(inetAddress.getAddress());
    }
    // IPv6：16 bytes for IPv6 address
    else if (IPV6_PATTERN.matcher(host).find()) {
      buf.writeByte(SocksAddressType.IPv6.byteValue());
      InetAddress inetAddress = InetAddress.getByName(host);
      buf.writeBytes(inetAddress.getAddress());
    }
    // domain：1 byte of ATYP + 1 byte of domain name length + 1–255 bytes of the domain name
    else {
      buf.writeByte(SocksAddressType.DOMAIN.byteValue());
      byte[] bytes = host.getBytes(StandardCharsets.UTF_8);
      buf.writeByte(bytes.length);
      buf.writeBytes(bytes);
    }
    // port
    buf.writeShort(address.getPort());
    ctx.writeAndFlush(buf);
  }

  /**
   * return msg format:
   * +----+-----+-------+------+----------+---------------+
   * |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.ATTR_PORT |
   * +----+-----+-------+------+----------+---------------+
   * | 1  |  1  | X'00' |  1   | Variable |      2        |
   * +----+-----+-------+------+----------+---------------+
   *
   * @param ctx
   * @param msg
   * @throws Exception
   */
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    byte ver = msg.readByte();
    byte rep = msg.readByte();

    if (ver != SocksVersion.SOCKS5.byteValue() || rep != Socks5CommandStatus.SUCCESS.byteValue()) {
      throw new ConnectException("connect failed: " + Socks5CommandStatus.valueOf(rep));
    }

    HttpOutBoundInitializer.addHttpOutBoundHandler(ctx.channel());
    ctx.pipeline().remove(socksConnHandler);

    ctx.pipeline().fireChannelActive();
  }

}
