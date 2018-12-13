package com.adolphor.mynety.server;

import com.adolphor.mynety.common.bean.Address;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.encryption.CryptFactory;
import com.adolphor.mynety.common.encryption.CryptUtil;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.utils.ByteStrUtils;
import com.adolphor.mynety.common.wrapper.AbstractSimpleHandler;
import com.adolphor.mynety.common.utils.SocksServerUtils;
import com.adolphor.mynety.server.config.Config;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.socks.SocksAddressType;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;

/**
 * 地址解析处理器
 * （可能会有黏包的问题，所以地址解析之后ByteBuf中还有数据）
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
@ChannelHandler.Sharable
public class AddressHandler extends AbstractSimpleHandler<ByteBuf> {

  public static final AddressHandler INSTANCE = new AddressHandler();

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.info("[ {}{}{} ] [AddressHandler-channelActive] channel active...", ctx.channel(), Constants.LOG_MSG, ctx.channel());
    ctx.channel().attr(Constants.ATTR_CRYPT_KEY).setIfAbsent(CryptFactory.get(Config.METHOD, Config.PASSWORD));
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    logger.debug("[ {}{} ] [AddressHandler-channelRead0] AddressHandler channelRead0 encrypted msg: {} readableBytes => {}", ctx.channel(), Constants.LOG_MSG, msg.readableBytes(), msg);
    ICrypt crypt = ctx.channel().attr(Constants.ATTR_CRYPT_KEY).get();

    if (msg.readableBytes() <= 0) {
      return;
    }
    ByteBuf dataBuff = Unpooled.buffer();
    dataBuff.writeBytes(CryptUtil.decrypt(crypt, msg));
    if (dataBuff.readableBytes() < 2) {
      return;
    }
    logger.debug("[ {}{} ][AddressHandler-channelRead0] decrypted msg: {} readableBytes => {}", ctx.channel(), Constants.LOG_MSG, dataBuff.readableBytes(), dataBuff);
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

      ByteBuf hostBytes = dataBuff.readBytes(hostLength);
      host = ByteStrUtils.getString(hostBytes);

      port = dataBuff.readShort();
    } else {
      throw new IllegalStateException("unknown address type: " + addressType);
    }
    logger.debug("[ {}{} ] [AddressHandler-channelRead0] parse address success: type={} => {}:{}", ctx.channel(), Constants.LOG_MSG, addressType, host, port);
    ctx.channel().attr(Constants.ATTR_REQUEST_ADDRESS).set(new Address(host, port));
    logger.debug("[ {}{} ] [AddressHandler-channelRead0] msg left after parse: {} readableBytes => {}", ctx.channel(), Constants.LOG_MSG, dataBuff.readableBytes(), dataBuff);
    ctx.channel().pipeline().addLast(new ConnectionHandler(dataBuff)); // 黏包的数据加入到请求缓存列表
    logger.info("[ {}{} ] [AddressHandler-channelRead0] add handler: ConnectionHandler", ctx.channel(), Constants.LOG_MSG);
    ctx.channel().pipeline().remove(this);
    logger.info("[ {}{} ] [AddressHandler-channelRead0] remove handler: AddressHandler", ctx.channel(), Constants.LOG_MSG);
    ctx.pipeline().fireChannelActive();
  }

  @Override
  protected void channelClose(ChannelHandlerContext ctx) {
    SocksServerUtils.closeOnFlush(ctx.channel());
  }

}