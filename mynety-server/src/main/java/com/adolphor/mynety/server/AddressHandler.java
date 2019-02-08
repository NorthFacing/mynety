package com.adolphor.mynety.server;

import com.adolphor.mynety.common.bean.Address;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.encryption.CryptFactory;
import com.adolphor.mynety.common.encryption.CryptUtil;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.utils.ByteStrUtils;
import com.adolphor.mynety.common.utils.ChannelUtils;
import com.adolphor.mynety.common.wrapper.AbstractSimpleHandler;
import com.adolphor.mynety.server.config.Config;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.socks.SocksAddressType;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;

import static com.adolphor.mynety.common.constants.Constants.ATTR_CRYPT_KEY;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_TEMP_MSG;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

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
    super.channelActive(ctx);

    ICrypt crypt = CryptFactory.get(Config.PROXY_METHOD, Config.PROXY_PASSWORD);
    ctx.channel().attr(ATTR_CRYPT_KEY).set(crypt);
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    logger.debug("[ {} ]【{}】收到客户端请求内容: {} bytes => {}", ctx.channel().id(), getSimpleName(this), msg.readableBytes(), msg);

    if (msg.readableBytes() <= 0) {
      return;
    }
    ByteBuf dataBuff = Unpooled.buffer();
    dataBuff.writeBytes(CryptUtil.decrypt(ctx.channel().attr(ATTR_CRYPT_KEY).get(), msg));
    if (dataBuff.readableBytes() < 2) {
      return;
    }
    logger.debug("[ {} ]【{}】解密之后的信息: {} bytes => {}", ctx.channel().id(), Constants.LOG_MSG, dataBuff.readableBytes(), dataBuff);
    String host;
    int port;
    int addressType = dataBuff.getUnsignedByte(0);
    if (addressType == SocksAddressType.IPv4.byteValue()) {
      if (dataBuff.readableBytes() < 7) {
        return;
      }
      dataBuff.readUnsignedByte();
      byte[] ipBytes = new byte[4];
      dataBuff.readBytes(ipBytes);
      host = InetAddress.getByAddress(ipBytes).getHostAddress();
      port = dataBuff.readShort();
    } else if (addressType == SocksAddressType.DOMAIN.byteValue()) {
      int hostLength = dataBuff.getUnsignedByte(1);
      if (dataBuff.readableBytes() < hostLength + 4) {
        return;
      }
      dataBuff.readUnsignedByte();
      dataBuff.readUnsignedByte();

      ByteBuf hostBytes = dataBuff.readBytes(hostLength);
      host = ByteStrUtils.getStringByDirectBuf(hostBytes);
      port = dataBuff.readUnsignedShort();
    } else {
      throw new Exception("unknown supported type: " + addressType);
    }
    logger.debug("[ {} ]【{}】解析出地址信息: type={} => {}:{}", ctx.channel().id(), getSimpleName(this), addressType, host, port);
    ctx.channel().attr(Constants.ATTR_REQUEST_ADDRESS).set(new Address(host, port));
    logger.debug("[ {} ]【{}】数据包中剩余的其他信息: {} bytes => {}", ctx.channel().id(), getSimpleName(this), dataBuff.readableBytes(), dataBuff);
    // 黏包的数据加入到请求缓存
    if (dataBuff.readableBytes() > 0) {
      ByteBuf temp = Unpooled.directBuffer(dataBuff.readableBytes()).writeBytes(dataBuff);
      ctx.channel().attr(ATTR_REQUEST_TEMP_MSG).get().set(temp);
      logger.debug("[ {} ]【{}】数据包中剩余信息添加到缓存，具体内容: {} bytes => {}", ctx.channel().id(), getSimpleName(this), ByteStrUtils.getArrByDirectBuf(temp.copy()).length, ByteStrUtils.getArrByDirectBuf(temp.copy()));
    }
    ctx.channel().pipeline().addLast(InBoundHandler.INSTANCE);
    logger.info("[ {} ]【{}】增加处理器: InBoundHandler", ctx.channel().id(), Constants.LOG_MSG);
    ctx.pipeline().remove(this);
    logger.info("[ {} ]【{}】移除处理器: AddressHandler", ctx.channel().id(), Constants.LOG_MSG);
    ctx.pipeline().fireChannelActive();
  }

  @Override
  protected void channelClose(ChannelHandlerContext ctx) {
    ChannelUtils.closeOnFlush(ctx.channel());
  }

}