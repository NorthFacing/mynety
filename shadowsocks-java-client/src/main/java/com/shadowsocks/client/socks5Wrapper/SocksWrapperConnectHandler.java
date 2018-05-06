/**
 * MIT License
 * <p>
 * Copyright (c) Bob.Zhu
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

import com.shadowsocks.client.httpAdapter.http_1_1.Http_1_1_2Socks5Handler;
import com.shadowsocks.client.httpAdapter.tunnel.HttpTunnel2Socks5Handler;
import com.shadowsocks.common.bean.Address;
import com.shadowsocks.common.constants.Constants;
import com.shadowsocks.common.nettyWrapper.AbstractSimpleHandler;
import com.shadowsocks.common.nettyWrapper.TempAbstractInRelayHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.socks.SocksAddressType;
import lombok.extern.slf4j.Slf4j;

import static com.shadowsocks.common.constants.Constants.CONNECTION_ESTABLISHED;
import static com.shadowsocks.common.constants.Constants.IPV4_PATTERN;
import static com.shadowsocks.common.constants.Constants.IPV6_PATTERN;
import static com.shadowsocks.common.constants.Constants.LOG_MSG;
import static com.shadowsocks.common.constants.Constants.LOG_MSG_IN;
import static com.shadowsocks.common.constants.Constants.REQUEST_ADDRESS;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;


/**
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public class SocksWrapperConnectHandler extends AbstractSimpleHandler<ByteBuf> {

  private final ByteBuf buf;
  private Channel clientChannel;
  private TempAbstractInRelayHandler inRelayHandler;

  /**
   * 发送的消息格式：
   * +----+-----+-------+------+----------+----------+
   * |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.ATTR_PORT |
   * +----+-----+-------+------+----------+----------+
   * | 1  |  1  | X'00' |  1   | Variable |    2     |
   * +----+-----+-------+------+----------+----------+
   */
  public SocksWrapperConnectHandler(TempAbstractInRelayHandler inRelayHandler, Channel clientChannel) {
    this.clientChannel = clientChannel;
    this.inRelayHandler = inRelayHandler;

    Address address = clientChannel.attr(REQUEST_ADDRESS).get();
    buf = Unpooled.buffer();
    buf.writeByte(0x05);
    buf.writeByte(0x01);
    buf.writeByte(0x00);
    String host = address.getHost();
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
    String addr = ByteBufUtil.hexDump(addrBuf);
    short port = msg.readShort();

    if (ver != 0X05 || cmd != 0x00) {
      logger.info("[ {}{}{} ]【socksWrapper】【连接】处理器收到响应消息内容错误：ver={}, cmd={}, psv={}, atyp={}, dstLen={}, addr={}, port={}",
          clientChannel, LOG_MSG, ctx.channel(), ver, cmd, psv, atyp, dstLen, addr, port);
      channelClose(ctx);
    } else {

      // 连接完成第一步，就是 消费缓存请求信息
      // TODO 放在这里处理嵌套太多，需要优化（弄个after connection 类来处理？状态机？）
      inRelayHandler.consumeHttpObjectsTemp();
      // 连接完成第二步，配置处理器，以及是否需要回复user-agent相关信息
      if (inRelayHandler instanceof Http_1_1_2Socks5Handler) {
        // ———— 3. 移除 inbound 和 outbound 双方的编解码（移除可以提效，不移除可以编辑请求头信息）
        clientChannel.pipeline().remove(HttpServerCodec.class);
        logger.debug("[ {}{}{} ] http1.1 clientChannel remove handler: HttpServerCodec", clientChannel, LOG_MSG, ctx.channel());
        ctx.channel().pipeline().remove(HttpClientCodec.class);
        logger.debug("[ {}{}{} ] http1.1 remoteChannel remove handler: HttpClientCodec", clientChannel, LOG_MSG, ctx.channel());
      } else if (inRelayHandler instanceof HttpTunnel2Socks5Handler) {
        // ———— 2. 告诉客户端建立隧道成功
        DefaultHttpResponse response = new DefaultHttpResponse(HTTP_1_1, CONNECTION_ESTABLISHED);
        clientChannel.writeAndFlush(response);
        logger.debug("[ {}{}{} ] httpTunnel connect socks success", clientChannel, LOG_MSG_IN, ctx.channel());
        // ———— 3. 移除 inbound 和 outbound 双方的编解码(tunnel代理如果没有增加ssl解析，那么就必须移除HTTP编解码器)
        clientChannel.pipeline().remove(HttpServerCodec.class);
        logger.debug("[ {}{}{} ] clientChannel remove handler: HttpServerCodec", clientChannel, LOG_MSG, ctx.channel());
        ctx.channel().pipeline().remove(HttpClientCodec.class);
        logger.debug("[ {}{}{} ] remoteChannel remove handler: HttpClientCodec", clientChannel, LOG_MSG, ctx.channel());
      }

      // socks5 连接并初始化成功，从现在开始可以使用此socks通道进行数据传输了
      inRelayHandler.setConnected(true);
      logger.info("[ {}{}{} ]【socksWrapper】【连接】处理器收到响应消息：ver={}, cmd={}, psv={}, atyp={}, dstLen={}, addr={}, port={}",
          clientChannel, LOG_MSG, ctx.channel(), ver, cmd, psv, atyp, dstLen, addr, port);
      // 移除socks5连接相关处理器
      ctx.pipeline().remove(this);
      // 激活下个handler的active方法，一般请情况下就是 remoteHandler 了
      ctx.fireChannelActive();
    }
  }

}
