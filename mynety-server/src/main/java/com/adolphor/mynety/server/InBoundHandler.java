package com.adolphor.mynety.server;

import com.adolphor.mynety.common.bean.Address;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.encryption.CryptUtil;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.utils.ByteStrUtils;
import com.adolphor.mynety.common.wrapper.AbstractInBoundHandler;
import com.adolphor.mynety.server.lan.LanAdapterInBoundHandler;
import com.adolphor.mynety.server.utils.LanPacFilter;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

import static com.adolphor.mynety.common.constants.Constants.ATTR_CRYPT_KEY;
import static com.adolphor.mynety.common.constants.Constants.ATTR_IN_RELAY_CHANNEL;
import static com.adolphor.mynety.common.constants.Constants.ATTR_OUT_RELAY_CHANNEL_REF;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_ADDRESS;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_TEMP_MSG;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG_OUT;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * 连接处理器，两种情形：
 * 1. socks代理的目标地址
 * 2. socks连接的内网穿透地址
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
@ChannelHandler.Sharable
public class InBoundHandler extends AbstractInBoundHandler<ByteBuf> {

  public static final InBoundHandler INSTANCE = new InBoundHandler();

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.info("[ {} ]【socks客户端激活】进入激活方法：{} ...", ctx.channel().id(), getSimpleName(this));
    super.channelActive(ctx);

    Address address = ctx.channel().attr(ATTR_REQUEST_ADDRESS).get();
    String dstAddr = address.getHost();
    Integer dstPort = address.getPort();

    boolean isDeny = LanPacFilter.isDeny(dstAddr);
    if (isDeny) {
      logger.warn("[ {}{} ]【socks客户端激活】拦截请求: {}:{}", ctx.channel().id(), Constants.LOG_MSG, dstAddr, dstPort);
      channelClose(ctx);
      return;
    }

    Bootstrap remoteBootStrap = new Bootstrap();

    // 是否进行内网穿透
    boolean isLanProxy = LanPacFilter.isLanProxy(dstAddr);
    // 内网穿透
    if (isLanProxy) {
      ctx.pipeline().addLast(LanAdapterInBoundHandler.INSTANCE);
      ctx.pipeline().remove(this);
      ctx.pipeline().fireChannelActive();
      return;
    }
    // socks代理
    else {
      remoteBootStrap.group(ctx.channel().eventLoop())
          .channel(Constants.channelClass)
          .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)
          .handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
              ch.pipeline().addLast(OutBoundHandler.INSTANCE);
              logger.info("[ {}{}{} ]【建立socks远程连接】增加处理器: OutBoundHandler", ctx.channel().id(), Constants.LOG_MSG, ch.id());
            }
          });
      try {
        ChannelFuture channelFuture = remoteBootStrap.connect(dstAddr, dstPort);
        channelFuture.addListener((ChannelFutureListener) future -> {
          if (future.isSuccess()) {
            Channel lanOutRelayChannel = future.channel();
            ctx.channel().attr(ATTR_OUT_RELAY_CHANNEL_REF).get().set(lanOutRelayChannel);
            lanOutRelayChannel.attr(ATTR_IN_RELAY_CHANNEL).set(ctx.channel());
            logger.debug("[ {}{}{} ]【建立socks远程连接】连接成功 => {}:{}", ctx.channel().id(), Constants.LOG_MSG_OUT, lanOutRelayChannel.id(), dstAddr, dstPort);
          } else {
            logger.warn("[ {}{} ]【建立socks远程连接】连接失败 => {}:{}", ctx.channel().id(), Constants.LOG_MSG, dstAddr, dstPort);
            logger.warn(ctx.channel().toString(), future.cause());
            future.cancel(true);
            channelClose(ctx);
          }
        });
      } catch (Exception e) {
        logger.error(ctx.channel().id() + LOG_MSG_OUT + " Send data to remoteServer error: ", e);
        channelClose(ctx);
      }
    }
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    AtomicReference<Channel> outRelayChannelRef = ctx.channel().attr(ATTR_OUT_RELAY_CHANNEL_REF).get();
    ICrypt crypt = ctx.channel().attr(ATTR_CRYPT_KEY).get();

    logger.debug("[ {}{}{} ]【{}】socks收到客户端请求消息: {} bytes => {}", ctx.channel().id(), Constants.LOG_MSG, (outRelayChannelRef.get() != null) ? outRelayChannelRef.get().id() : "", getSimpleName(this), msg.readableBytes(), ByteStrUtils.getArrByDirectBuf(msg.copy()));
    byte[] decrypt = CryptUtil.decrypt(crypt, msg);
    logger.debug("[ {}{}{} ]【{}】socks收到客户端请求消息解密之后的内容: {} bytes => {}", ctx.channel().id(), Constants.LOG_MSG, (outRelayChannelRef.get() != null) ? outRelayChannelRef.get().id() : "", getSimpleName(this), decrypt.length, decrypt);
    if (outRelayChannelRef.get() != null) {
      logger.debug("[ {}{}{} ]【{}】socks收到客户端请求消息并发送到请求地址: {} bytes => {}", ctx.channel().id(), Constants.LOG_MSG_OUT, outRelayChannelRef.get().id(), getSimpleName(this), decrypt.length, decrypt);
      outRelayChannelRef.get().writeAndFlush(Unpooled.wrappedBuffer(decrypt));
    } else {
      AtomicReference tempMsgRef = ctx.channel().attr(ATTR_REQUEST_TEMP_MSG).get();
      if (tempMsgRef.get() != null) {
        ByteBuf byteBuf = (ByteBuf) tempMsgRef.get();
        ByteBuf newBuf = Unpooled.buffer(byteBuf.readableBytes() + decrypt.length)
            .writeBytes(byteBuf)
            .writeBytes(decrypt);
        tempMsgRef.set(newBuf);
        logger.debug("[ {}{} ]【{}】socks收到客户端请求消息并追加到缓存: {} bytes => {}", ctx.channel().id(), Constants.LOG_MSG, getSimpleName(this), decrypt.length, decrypt);
      } else {
        tempMsgRef.set(Unpooled.wrappedBuffer(decrypt));
        logger.debug("[ {}{} ]【{}】socks收到客户端请求消息并新建缓存: {} bytes => {}", ctx.channel().id(), Constants.LOG_MSG, getSimpleName(this), decrypt.length, decrypt);
      }
    }
  }

}
