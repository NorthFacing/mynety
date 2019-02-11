package com.adolphor.mynety.server;

import com.adolphor.mynety.common.bean.Address;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.wrapper.AbstractInBoundHandler;
import com.adolphor.mynety.server.lan.LanAdapterInBoundHandler;
import com.adolphor.mynety.server.utils.LanPacFilter;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
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
import static com.adolphor.mynety.common.constants.Constants.CONNECT_TIMEOUT;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG_OUT;

/**
 * Handle client request msg, three situations:
 * 1. deny the request and close the channel
 * 2. send msg to remote server
 * 3. send msg to lan proxy
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
    super.channelActive(ctx);

    Address address = ctx.channel().attr(ATTR_REQUEST_ADDRESS).get();
    String dstAddr = address.getHost();
    Integer dstPort = address.getPort();

    boolean isDeny = LanPacFilter.isDeny(dstAddr);
    if (isDeny) {
      channelClose(ctx);
      return;
    }

    Bootstrap remoteBootStrap = new Bootstrap();

    boolean isLanProxy = LanPacFilter.isLanProxy(dstAddr);
    if (isLanProxy) {
      ctx.pipeline().addLast(LanAdapterInBoundHandler.INSTANCE);
      ctx.pipeline().remove(this);
      ctx.pipeline().fireChannelActive();
      return;
    }
    remoteBootStrap.group(ctx.channel().eventLoop())
        .channel(Constants.channelClass)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT)
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast(OutBoundHandler.INSTANCE);
          }
        });
    try {
      remoteBootStrap.connect(dstAddr, dstPort).addListener((ChannelFutureListener) future -> {
        if (future.isSuccess()) {
          Channel lanOutRelayChannel = future.channel();
          ctx.channel().attr(ATTR_OUT_RELAY_CHANNEL_REF).get().set(lanOutRelayChannel);
          lanOutRelayChannel.attr(ATTR_IN_RELAY_CHANNEL).set(ctx.channel());
        } else {
          logger.error(ctx.channel().toString(), future.cause().getMessage());
          channelClose(ctx);
        }
      });
    } catch (Exception e) {
      logger.error(ctx.channel().id() + LOG_MSG_OUT + " Send data to remoteServer error: ", e);
      channelClose(ctx);
    }
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    AtomicReference<Channel> outRelayChannelRef = ctx.channel().attr(ATTR_OUT_RELAY_CHANNEL_REF).get();
    ICrypt crypt = ctx.channel().attr(ATTR_CRYPT_KEY).get();

    ByteBuf decryptBuf = crypt.decrypt(msg);
    if (outRelayChannelRef.get() != null) {
      outRelayChannelRef.get().writeAndFlush(decryptBuf);
      return;
    }

    AtomicReference tempMsgRef = ctx.channel().attr(ATTR_REQUEST_TEMP_MSG).get();
    if (tempMsgRef.get() == null) {
      tempMsgRef.set(decryptBuf);
      return;
    }

    ByteBuf tempBuf = (ByteBuf) tempMsgRef.get();
    if (tempBuf.isWritable()) {
      tempBuf.writeBytes(msg);
    } else {
      ByteBuf byteBuf = Unpooled.buffer().writeBytes(tempBuf);
      byteBuf.writeBytes(msg);
      tempMsgRef.set(byteBuf);
    }

  }

}
