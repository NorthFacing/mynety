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
package com.shadowsocks.server;

import com.shadowsocks.common.bean.Address;
import com.shadowsocks.common.encryption.CryptFactory;
import com.shadowsocks.common.encryption.CryptUtil;
import com.shadowsocks.common.encryption.ICrypt;
import com.shadowsocks.common.nettyWrapper.AbstractSimpleHandler;
import com.shadowsocks.server.Config.Config;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.socks.SocksAddressType;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;

import static com.shadowsocks.common.constants.Constants.ATTR_CRYPT_KEY;
import static com.shadowsocks.common.constants.Constants.LOG_MSG;
import static com.shadowsocks.common.constants.Constants.REQUEST_ADDRESS;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * 地址解析处理器
 * （可能会有黏包的问题，所以地址解析之后ByteBuf中还有数据）
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Slf4j
@ChannelHandler.Sharable
public class AddressHandler extends AbstractSimpleHandler<ByteBuf> {

  public static final AddressHandler INSTANCE = new AddressHandler();

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.info("[ {}{}{} ] {} channel active...", ctx.channel(), LOG_MSG, ctx.channel(), getSimpleName(this));
    ctx.channel().attr(ATTR_CRYPT_KEY).setIfAbsent(CryptFactory.get(Config.METHOD, Config.PASSWORD));
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    logger.debug("[ {}{} ] AddressHandler channelRead0 encrypted msg: {} readableBytes => {}", ctx.channel(), LOG_MSG, msg.readableBytes(), msg);
    ICrypt crypt = ctx.channel().attr(ATTR_CRYPT_KEY).get();

    if (msg.readableBytes() <= 0) {
      return;
    }
    ByteBuf dataBuff = Unpooled.buffer();
    dataBuff.writeBytes(CryptUtil.decrypt(crypt, msg));
    if (dataBuff.readableBytes() < 2) {
      return;
    }
    logger.debug("[ {}{} ] AddressHandler channelRead0 decrypted msg: {} readableBytes => {}", ctx.channel(), LOG_MSG, dataBuff.readableBytes(), dataBuff);
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
    logger.debug("[ {}{} ] AddressHandler parse address success: type={} => {}:{}", ctx.channel(), LOG_MSG, addressType, host, port);
    ctx.channel().attr(REQUEST_ADDRESS).set(new Address(host, port));
    logger.debug("[ {}{} ] AddressHandler channelRead0 msg left after parse: {} readableBytes => {}", ctx.channel(), LOG_MSG, dataBuff.readableBytes(), dataBuff);
    ctx.channel().pipeline().addLast(new ConnectionHandler(dataBuff.retain())); // 黏包的数据加入到请求缓存列表
    logger.info("[ {}{} ] add handler: ConnectionHandler", ctx.channel(), LOG_MSG);
    ctx.channel().pipeline().remove(this);
    logger.info("[ {}{} ] remove handler: AddressHandler", ctx.channel(), LOG_MSG);
    ctx.pipeline().fireChannelActive();
  }

}