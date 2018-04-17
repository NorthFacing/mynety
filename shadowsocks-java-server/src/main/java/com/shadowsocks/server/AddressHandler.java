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
package com.shadowsocks.server;

import com.shadowsocks.common.encryption.CryptFactory;
import com.shadowsocks.common.encryption.CryptUtil;
import com.shadowsocks.common.encryption.ICrypt;
import com.shadowsocks.server.Config.Config;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socks.SocksAddressType;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;

/**
 * 地址解析处理器
 *
 * @author 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Slf4j
@ChannelHandler.Sharable
public class AddressHandler extends SimpleChannelInboundHandler {

  public static final AddressHandler INSTANCE = new AddressHandler();

  public AddressHandler() {
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    ctx.channel().attr(Config.CRYPT_KEY).setIfAbsent(CryptFactory.get(Config.METHOD, Config.PASSWORD));
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ctx.close();
    log.error("AddressHandler error", cause);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    ctx.close();
    log.info("AddressHandler channelInactive close");
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    ICrypt crypt = ctx.channel().attr(Config.CRYPT_KEY).get();

    ByteBuf buff = (ByteBuf) msg;

    if (buff.readableBytes() <= 0) {
      return;
    }
    ByteBuf dataBuff = Unpooled.buffer();
    dataBuff.writeBytes(CryptUtil.decrypt(crypt, msg));
    if (dataBuff.readableBytes() < 2) {
      return;
    }
    String host = null;
    int port = 0;
    int addressType = dataBuff.getUnsignedByte(0);
    if (addressType == SocksAddressType.IPv4.byteValue()) {
      if (dataBuff.readableBytes() < 7) {
        return;
      }
      dataBuff.readUnsignedByte();
      byte[] ipBytes = new byte[4];
      dataBuff.readBytes(ipBytes);
      host = InetAddress.getByAddress(ipBytes).toString().substring(1);
      port = dataBuff.readShort();
    } else if (addressType == SocksAddressType.DOMAIN.byteValue()) {
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
    } else {
      throw new IllegalStateException("unknown address type: " + addressType);
    }
    log.debug("addressType = " + addressType + ",host = " + host + ",port = " + port);
    ctx.channel().attr(Config.HOST).set(host);
    ctx.channel().attr(Config.PORT).set(port);
    ctx.channel().attr(Config.BUF).set(dataBuff);

    ctx.channel().pipeline().remove(this);
    ctx.channel().pipeline().addLast(new ConnectionHandler());
    ctx.pipeline().fireChannelActive();
  }
}