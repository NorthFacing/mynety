package com.adolphor.mynety.client.adapter;

import com.adolphor.mynety.client.http.HttpOutBoundHandler;
import com.adolphor.mynety.common.bean.Address;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.utils.ByteStrUtils;
import com.adolphor.mynety.common.wrapper.AbstractSimpleHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.socks.SocksAddressType;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import static com.adolphor.mynety.common.constants.Constants.ATTR_IN_RELAY_CHANNEL;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_ADDRESS;
import static com.adolphor.mynety.common.constants.Constants.IPV4_PATTERN;
import static com.adolphor.mynety.common.constants.Constants.IPV6_PATTERN;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG;
import static com.adolphor.mynety.common.constants.Constants.RESERVED_BYTE;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

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
   * 发送的消息格式：
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
    // 如果是IPv4：4 bytes for IPv4 address
    if (IPV4_PATTERN.matcher(host).find()) {
      buf.writeByte(SocksAddressType.IPv4.byteValue());
      InetAddress inetAddress = InetAddress.getByName(host);
      buf.writeBytes(inetAddress.getAddress());
    }
    // 如果是IPv6：16 bytes for IPv6 address
    else if (IPV6_PATTERN.matcher(host).find()) {
      buf.writeByte(SocksAddressType.IPv6.byteValue());
      InetAddress inetAddress = InetAddress.getByName(host);
      buf.writeBytes(inetAddress.getAddress());
    }
    // 如果是域名：1 byte of ATYP + 1 byte of domain name length + 1–255 bytes of the domain name
    else {
      buf.writeByte(SocksAddressType.DOMAIN.byteValue());
      byte[] bytes = host.getBytes(StandardCharsets.UTF_8);
      buf.writeByte(bytes.length);
      buf.writeBytes(bytes);
    }
    // port
    buf.writeShort(address.getPort());
    logger.info("[ {}{}{} ]【连接】处理器激活，发送连接请求：{}", inRelayChannel.id(), Constants.LOG_MSG_OUT, ctx.channel().id(), ByteBufUtil.hexDump(buf));
    ctx.writeAndFlush(buf);
  }

  /**
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
    Channel inRelayChannel = ctx.channel().attr(ATTR_IN_RELAY_CHANNEL).get();
    byte ver = msg.readByte();
    byte rep = msg.readByte();
    byte psv = msg.readByte();
    byte atyp = msg.readByte();

    if (ver != SocksVersion.SOCKS5.byteValue() || rep != Socks5CommandStatus.SUCCESS.byteValue()) {
      logger.info("[ {}{}{} ]【连接】处理器收到响应消息内容错误：ver={}, rep={}, psv={}, atyp={}", inRelayChannel.id(), LOG_MSG, ctx.channel().id(), ver, rep, psv, atyp);
      channelClose(ctx);
      return;
    }

    String addr;
    int port;
    if (atyp == SocksAddressType.DOMAIN.byteValue()) {
      byte dstLen = msg.readByte();
      ByteBuf addrBuf = msg.readBytes(dstLen);
      addr = ByteStrUtils.getString(addrBuf);
      port = msg.readShort();
    } else if (atyp == SocksAddressType.IPv4.byteValue()) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 4; i++) {
        sb.append(msg.readUnsignedByte()).append(".");
      }
      addr = sb.toString();
      port = msg.readShort();
    } else if (atyp == SocksAddressType.IPv6.byteValue()) {
      logger.info("[ {}{}{} ]【连接】处理器收到响应消息，不支持IPv6类型：ver={}, rep={}, psv={}, atyp={}", inRelayChannel.id(), LOG_MSG, ctx.channel().id(), ver, rep, psv, atyp);
      channelClose(ctx);
      return;
    } else {
      logger.info("[ {}{}{} ]【连接】处理器收到响应消息，不支持的HOST类型：ver={}, rep={}, psv={}, atyp={}", inRelayChannel.id(), LOG_MSG, ctx.channel().id(), ver, rep, psv, atyp);
      channelClose(ctx);
      return;
    }
    // socks5 连接并初始化成功，从现在开始可以使用此socks通道进行数据传输了
    logger.info("[ {}{}{} ]【连接】处理器收到响应消息：ver={}, rep={}, psv={}, atyp={}, addr={}, port={}", inRelayChannel.id(), LOG_MSG, ctx.channel().id(), ver, rep, psv, atyp, addr, port);

    ctx.channel().pipeline().addLast(new HttpClientCodec());
    logger.info("[ {} ]【{}】增加处理器: HttpClientCodec", ctx.channel().id(), getSimpleName(this));
    ctx.pipeline().addLast(new HttpObjectAggregator(6553600));
    logger.info("[ {} ]【{}】增加处理器: HttpObjectAggregator", ctx.channel().id(), getSimpleName(this));
    ctx.channel().pipeline().addLast(HttpOutBoundHandler.INSTANCE);
    logger.info("[ {} ]【{}】增加处理器: HttpOutBoundHandler", ctx.channel().id(), getSimpleName(this));
    ctx.pipeline().remove(this);
    logger.info("[ {}{}{} ]【{}】移除处理器: SocksConnHandler", inRelayChannel.id(), LOG_MSG, ctx.channel().id(), getSimpleName(this));
    logger.info("[ {}{}{} ]【{}】连接成功：what's done is done", inRelayChannel.id(), LOG_MSG, ctx.channel().id(), getSimpleName(this));
    // 调用 HttpOutBoundHandler 的 channelActive 方法发送缓存数据（缓存的是数据是编解码之后可以直接发送的数据）
    ctx.fireChannelActive();

  }

}
