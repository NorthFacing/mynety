package com.adolphor.mynety.server;

import com.adolphor.mynety.common.bean.Address;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.encryption.CryptUtil;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.wrapper.AbstractInRelayHandler;
import com.adolphor.mynety.server.lan.LanAdapterHandler;
import com.adolphor.mynety.server.utils.LanPacFilter;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.ATTR_CRYPT_KEY;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_ADDRESS;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_TEMP_LIST;

/**
 * 连接处理器，两种情形：
 * 1. socks代理的目标地址
 * 2. socks连接的内网穿透地址
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Slf4j
public class ConnectionHandler extends AbstractInRelayHandler<ByteBuf> {

  public ConnectionHandler(ByteBuf msg) {
    if (msg.readableBytes() > 0) {
      requestTempLists.add(msg.retain());
      logger.debug("[ {} ] [ConnectionHandler-constructor] add socks client request to temp list: {}", Constants.LOG_MSG, msg);
    } else {
      logger.debug("[ {} ] [ConnectionHandler-constructor] discard empty msg: {}", Constants.LOG_MSG, msg);
    }
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.info("[ {}{}{} ] [ConnectionHandler-channelActive] channel active...", ctx.channel(), Constants.LOG_MSG, remoteChannelRef.get());

    Channel clientChannel = ctx.channel();
    clientChannel.attr(ATTR_REQUEST_TEMP_LIST).set(requestTempLists);

    ICrypt crypt = clientChannel.attr(ATTR_CRYPT_KEY).get();
    Address address = clientChannel.attr(ATTR_REQUEST_ADDRESS).get();
    String dstAddr = address.getHost();
    Integer dstPort = address.getPort();

    boolean isDeny = LanPacFilter.isDeny(dstAddr);
    if (isDeny) {
      logger.warn("[ {}{} ] lan dst address is configured to be shutdown: {}:{}", ctx.channel(), Constants.LOG_MSG, dstAddr, dstPort);
      channelClose(ctx);
      return;
    }

    Bootstrap remoteBootStrap = new Bootstrap();

    // 是否进行内网穿透
    boolean isProxy = LanPacFilter.isProxy(dstAddr);
    // 内网穿透
    if (isProxy) {
      ctx.pipeline().addAfter(ctx.name(), null, new LanAdapterHandler());
      ctx.pipeline().remove(this);
      ctx.pipeline().fireChannelActive();
      return;
    }
    // socks代理
    else {
      remoteBootStrap.group(clientChannel.eventLoop())
          .channel(Constants.channelClass)
          .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)
          .handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
              ch.pipeline().addLast(new RemoteHandler(ctx.channel(), crypt));
              logger.info("[ {}{}{} ] [ConnectionHandler-channelActive] socks out pipeline add handler: RemoteHandler", ctx.channel(), Constants.LOG_MSG, ch);
            }
          });
      try {
        ChannelFuture channelFuture = remoteBootStrap.connect(dstAddr, dstPort);
        channelFuture.addListener((ChannelFutureListener) future -> {
          if (future.isSuccess()) {
            remoteChannelRef.set(future.channel());
            logger.debug("{}{} [ConnectionHandler-channelActive] socks server connect to dst host success => {}:{}", Constants.LOG_MSG_OUT, clientChannel, dstAddr, dstPort);
          } else {
            logger.debug("{}{} [ConnectionHandler-channelActive] socks server connect to dst host failed => {}:{}", Constants.LOG_MSG, clientChannel, dstAddr, dstPort);
            future.cancel(true);
            channelClose(ctx);
          }
        });
      } catch (Exception e) {
        logger.error(Constants.LOG_MSG + " socks server connect internet error", e);
        channelClose(ctx);
      }
    }

  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    Channel remoteChannel = remoteChannelRef.get();
    logger.debug("[ {}{}{} ] [ConnectionHandler-channelRead0] received msg: {} bytes => {}", ctx.channel(), Constants.LOG_MSG, remoteChannel, msg.readableBytes(), msg);
    ICrypt crypt = ctx.channel().attr(ATTR_CRYPT_KEY).get();
    byte[] temp = CryptUtil.decrypt(crypt, msg);
    ByteBuf decryptBuf = Unpooled.wrappedBuffer(temp);
    logger.debug("[ {}{}{} ] [ConnectionHandler-channelRead0] msg after decrypt: {} bytes => {}", ctx.channel(), Constants.LOG_MSG, remoteChannel, decryptBuf.readableBytes(), decryptBuf);
    synchronized (requestTempLists) {
      if (remoteChannel != null) {
        remoteChannel.writeAndFlush(decryptBuf);
        logger.debug("[ {}{}{} ] [ConnectionHandler-channelRead0] write msg to dst host channel: {} bytes => {}", ctx.channel(), Constants.LOG_MSG_OUT, remoteChannelRef.get(), decryptBuf.readableBytes(), decryptBuf);
      } else {
        requestTempLists.add(decryptBuf);
        logger.debug("[ {}{}{} ] [ConnectionHandler-channelRead0] add msg to temp list: {}", ctx.channel(), Constants.LOG_MSG, remoteChannel, msg);
      }
    }
  }

}
