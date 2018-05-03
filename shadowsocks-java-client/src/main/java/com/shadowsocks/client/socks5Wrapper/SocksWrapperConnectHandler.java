/**
 * MIT License
 * <p>
 * Copyright (c) 2018 0haizhu0@gmail.com
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.shadowsocks.client.socks5Wrapper;

import com.shadowsocks.common.bean.FullPath;
import com.shadowsocks.common.constants.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socks.SocksAddressType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.shadowsocks.common.constants.Constants.EXTRA_OUT_RELAY_HANDLER;
import static com.shadowsocks.common.constants.Constants.HTTP_REQUEST;
import static com.shadowsocks.common.constants.Constants.HTTP_REQUEST_FULLPATH;
import static com.shadowsocks.common.constants.Constants.IPV4_PATTERN;
import static com.shadowsocks.common.constants.Constants.IPV6_PATTERN;
import static com.shadowsocks.common.constants.Constants.SOCKS5_CONNECTED;

/**
 * @author 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public class SocksWrapperConnectHandler extends SimpleChannelInboundHandler {

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
  public SocksWrapperConnectHandler(Channel clientChannel) {
    this.clientChannel = clientChannel;
    FullPath fullPath = clientChannel.attr(HTTP_REQUEST_FULLPATH).get();
    buf = Unpooled.buffer();
    buf.writeByte(0x05);
    buf.writeByte(0x01);
    buf.writeByte(0x00);
    String host = fullPath.getHost();
    byte[] bytes = host.getBytes();
    if (IPV4_PATTERN.matcher(host).find()) {
      buf.writeByte(SocksAddressType.IPv4.byteValue());
    } else if (IPV6_PATTERN.matcher(host).find()) {
      buf.writeByte(SocksAddressType.IPv6.byteValue());
    } else {
      buf.writeByte(SocksAddressType.DOMAIN.byteValue());
      buf.writeByte(bytes.length); // ADDR.LEN
    }
    buf.writeBytes(bytes); // ADDR.LEN
    buf.writeShort(fullPath.getPort()); // port
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    logger.info("{} {}【socksWrapper】【连接】处理器激活，发送连接请求：{}", Constants.LOG_MSG, ctx.channel(), ByteBufUtil.hexDump(buf));
    ctx.channel().writeAndFlush(buf);
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
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    ByteBuf result = (ByteBuf) msg;
    byte ver = result.readByte();
    byte cmd = result.readByte();
    byte psv = result.readByte();
    byte atyp = result.readByte();

    byte dstLen = result.readByte();
    ByteBuf addrBuf = result.readBytes(dstLen);
    String addr = ByteBufUtil.hexDump(addrBuf);
    short port = result.readShort();

    if (ver != 0X05 || cmd != 0x00) {
      logger.info("{} {}【socksWrapper】【连接】处理器收到响应消息内容错误：ver={}, cmd={}, psv={}, atyp={}, dstLen={}, addr={}, port={}",
          Constants.LOG_MSG, ctx.channel(), ver, cmd, psv, atyp, dstLen, addr, port);
      ctx.close();
    } else {
      // socks5 连接并初始化成功，从现在开始可以使用此socks通道进行数据传输了
      ctx.channel().attr(SOCKS5_CONNECTED).set(true);
      logger.info("{} {}【socksWrapper】【连接】处理器收到响应消息：ver={}, cmd={}, psv={}, atyp={}, dstLen={}, addr={}, port={}",
          Constants.LOG_MSG, ctx.channel(), ver, cmd, psv, atyp, dstLen, addr, port);
    }

  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    // 连接成功之后，配置盲转时需要的处理器
    List<ChannelHandler> handlers = clientChannel.attr(EXTRA_OUT_RELAY_HANDLER).get();
    handlers.forEach(handler -> ctx.pipeline().addAfter(ctx.name(), null, handler));
    // 移除socks5连接相关处理器
    ctx.pipeline().remove(this);
    logger.info("{} {}【socksWrapper】【连接】处理器任务完成，移除此处理器", Constants.LOG_MSG, ctx.channel());
    // socks5 连接建立成功，将消息放回pipeline进行盲转
    clientChannel.pipeline().fireChannelRead(clientChannel.attr(HTTP_REQUEST).get());
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
    logger.error(Constants.LOG_MSG + ctx.channel() + "【socksWrapper】【连接】处理器异常：", throwable);
    ctx.channel().close();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    logger.info("{} {}【socksWrapper】【连接】处理器连接断开：{}", Constants.LOG_MSG, ctx.channel(), ctx.channel());
    super.channelInactive(ctx);
  }


}
