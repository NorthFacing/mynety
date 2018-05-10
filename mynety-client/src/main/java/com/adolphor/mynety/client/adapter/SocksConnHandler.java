package com.adolphor.mynety.client.adapter;

import com.adolphor.mynety.common.bean.Address;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.utils.ByteStrUtils;
import com.adolphor.mynety.common.wrapper.AbstractSimpleHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.socks.SocksAddressType;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_ADDRESS;
import static com.adolphor.mynety.common.constants.Constants.IPV4_PATTERN;
import static com.adolphor.mynety.common.constants.Constants.IPV6_PATTERN;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG;

/**
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public class SocksConnHandler extends AbstractSimpleHandler<ByteBuf> {

  private final ByteBuf buf;
  private Channel clientChannel;

  /**
   * 发送的消息格式：
   * +----+-----+-------+------+----------+----------+
   * |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.ATTR_PORT |
   * +----+-----+-------+------+----------+----------+
   * | 1  |  1  | X'00' |  1   | Variable |    2     |
   * +----+-----+-------+------+----------+----------+
   */
  public SocksConnHandler(Channel clientChannel) {
    this.clientChannel = clientChannel;

    Address address = clientChannel.attr(ATTR_REQUEST_ADDRESS).get();
    buf = Unpooled.buffer();
    buf.writeByte(0x05);
    buf.writeByte(0x01);
    buf.writeByte(0x00);
    String host = address.getHost();
    byte[] bytes = ByteStrUtils.getByteArr(host);
    if (IPV4_PATTERN.matcher(host).find()) {
      buf.writeByte(SocksAddressType.IPv4.byteValue());
    } else if (IPV6_PATTERN.matcher(host).find()) {
      buf.writeByte(SocksAddressType.IPv6.byteValue());
    } else {
      buf.writeByte(SocksAddressType.DOMAIN.byteValue());
      buf.writeByte(bytes.length); // ADDR.LEN
    }
    buf.writeBytes(bytes); // ADDR.LEN
    buf.writeShort(address.getPort()); // port
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    logger.info("[ {}{}{} ]【socksWrapper】【连接】处理器激活，发送连接请求：{}", clientChannel, Constants.LOG_MSG_OUT, ctx.channel(), ByteBufUtil.hexDump(buf));
    ctx.writeAndFlush(buf);
  }

  /**
   * +----+-----+-------+------+----------+----------+
   * |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.ATTR_PORT |
   * +----+-----+-------+------+----------+----------+
   * | 1  |  1  | X'00' |  1   | Variable |    2     |
   * +----+-----+-------+------+----------+----------+
   *
   * @param ctx
   * @param msg
   * @throws Exception
   */
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    byte ver = msg.readByte();
    byte cmd = msg.readByte();
    byte psv = msg.readByte();
    byte atyp = msg.readByte();

    byte dstLen = msg.readByte();
    ByteBuf addrBuf = msg.readBytes(dstLen);
    String addr = ByteStrUtils.getString(addrBuf);
    short port = msg.readShort();

    if (ver != 0X05 || cmd != 0x00) {
      logger.info("[ {}{}{} ]【socksWrapper】【连接】处理器收到响应消息内容错误：ver={}, cmd={}, psv={}, atyp={}, dstLen={}, addr={}, port={}",
          clientChannel, LOG_MSG, ctx.channel(), ver, cmd, psv, atyp, dstLen, addr, port);
      channelClose(ctx);
    } else {
      // socks5 连接并初始化成功，从现在开始可以使用此socks通道进行数据传输了
      logger.info("[ {}{}{} ]【socksWrapper】【连接】处理器收到响应消息：ver={}, cmd={}, psv={}, atyp={}, dstLen={}, addr={}, port={}",
          clientChannel, LOG_MSG, ctx.channel(), ver, cmd, psv, atyp, dstLen, addr, port);
      // 移除socks5连接相关处理器
      ctx.pipeline().remove(this);
      logger.info("[ {}{}{} ] 【socksWrapper】remove handlers: SocksConnHandler", clientChannel, LOG_MSG, ctx.channel());
      // 调用remoteHandler的 channelActive方法发送缓存数据（缓存的是数据是编解码之后可以直接发送的数据）
      ctx.pipeline().fireChannelActive();
      logger.info("[ {}{}{} ] 【socksWrapper】what's done is done, the remote channelActive method will run next...", clientChannel, LOG_MSG, ctx.channel());
    }
  }

}
