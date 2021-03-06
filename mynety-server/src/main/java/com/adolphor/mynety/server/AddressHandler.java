package com.adolphor.mynety.server;

import com.adolphor.mynety.common.bean.Address;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.encryption.CryptFactory;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.utils.ByteStrUtils;
import com.adolphor.mynety.common.wrapper.AbstractSimpleHandler;
import com.adolphor.mynety.server.config.Config;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.socks.SocksAddressType;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;

import static com.adolphor.mynety.common.constants.Constants.ATTR_CRYPT_KEY;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_TEMP_MSG;
import static com.adolphor.mynety.common.constants.HandlerName.addressHandler;
import static com.adolphor.mynety.common.constants.HandlerName.inBoundHandler;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * at first, parse the request address; if there are msg left in the buf, then put to caches
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
    super.channelActive(ctx);
    ICrypt crypt = CryptFactory.get(Config.PROXY_METHOD, Config.PROXY_PASSWORD);
    ctx.channel().attr(ATTR_CRYPT_KEY).set(crypt);
  }

  /**
   * if get exception, then the request is a illegal request
   *
   * @param ctx
   * @param msg
   */
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    if (msg.readableBytes() <= 0) {
      throw new IllegalAccessException("date length is too short ...");
    }
    ByteBuf dataBuff = Unpooled.buffer();
    dataBuff.writeBytes(ctx.channel().attr(ATTR_CRYPT_KEY).get().decrypt(msg));
    if (dataBuff.readableBytes() < 2) {
      throw new IllegalAccessException("date length is too short ...");
    }
    String host;
    int port;
    int addressType = dataBuff.getUnsignedByte(0);
    if (addressType == SocksAddressType.IPv4.byteValue()) {
      if (dataBuff.readableBytes() < 7) {
        throw new IllegalAccessException("date length is too short ...");
      }
      dataBuff.readUnsignedByte();
      byte[] ipBytes = new byte[4];
      dataBuff.readBytes(ipBytes);
      host = InetAddress.getByAddress(ipBytes).getHostAddress();
      port = dataBuff.readShort();
    } else if (addressType == SocksAddressType.DOMAIN.byteValue()) {
      int hostLength = dataBuff.getUnsignedByte(1);
      if (dataBuff.readableBytes() < hostLength + 4) {
        throw new IllegalAccessException("date length is too short ...");
      }
      dataBuff.readUnsignedByte();
      dataBuff.readUnsignedByte();

      ByteBuf hostBytes = dataBuff.readBytes(hostLength);
      host = ByteStrUtils.readStringByBuf(hostBytes);
      // avoid resource leak
      hostBytes.release();
      port = dataBuff.readUnsignedShort();
    } else {
      throw new IllegalAccessException("unknown supported type: " + addressType);
    }
    logger.info("new request to {}:{} ...", host, port);
    ctx.channel().attr(Constants.ATTR_REQUEST_ADDRESS).set(new Address(host, port));
    if (dataBuff.readableBytes() > 0) {
      ReferenceCountUtil.retain(dataBuff);
      ctx.channel().attr(ATTR_REQUEST_TEMP_MSG).get().set(dataBuff);
      logger.debug("[ {} ] {} add msg to cache => {} ", ctx.channel().id(), getSimpleName(this), dataBuff);
    }
    ctx.channel().pipeline().addAfter(addressHandler, inBoundHandler, InBoundHandler.INSTANCE);
    ctx.pipeline().remove(addressHandler);
    ctx.pipeline().fireChannelActive();
  }

}