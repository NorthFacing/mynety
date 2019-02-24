package com.adolphor.mynety.lan;

import com.adolphor.mynety.common.bean.lan.LanMessage;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.encryption.CryptFactory;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.utils.ByteStrUtils;
import com.adolphor.mynety.common.utils.LanMsgUtils;
import com.adolphor.mynety.common.wrapper.AbstractInBoundHandler;
import com.adolphor.mynety.lan.config.Config;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.ConnectException;
import java.security.InvalidAlgorithmParameterException;
import java.util.concurrent.atomic.AtomicReference;

import static com.adolphor.mynety.common.constants.Constants.ATTR_CRYPT_KEY;
import static com.adolphor.mynety.common.constants.Constants.ATTR_IN_RELAY_CHANNEL;
import static com.adolphor.mynety.common.constants.Constants.ATTR_OUT_RELAY_CHANNEL_REF;
import static com.adolphor.mynety.common.constants.Constants.COLON;
import static com.adolphor.mynety.common.constants.Constants.CONNECT_TIMEOUT;
import static com.adolphor.mynety.common.constants.LanConstants.ATTR_LAST_BEAT_NO;
import static com.adolphor.mynety.common.constants.LanConstants.IS_MAIN_CHANNEL;

/**
 * connect to lanServer
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
@ChannelHandler.Sharable
public class InBoundHandler extends AbstractInBoundHandler<LanMessage> {

  public static final InBoundHandler INSTANCE = new InBoundHandler();

  /**
   * @param ctx
   * @param msg
   * @throws Exception
   */
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, LanMessage msg) throws InvalidAlgorithmParameterException, IOException {
    switch (msg.getType()) {
      case CONNECT:
        handleConnectionMessage(ctx, msg);
        break;
      case TRANSMIT:
        handleTransferMessage(ctx, msg);
        break;
      case HEARTBEAT:
        handleHeartbeatMessage(ctx, msg);
        break;
      default:
        logger.info("[ {} ] {} unsupported msg type: {}", ctx.channel(), msg);
        break;
    }
  }

  /**
   * receive and sent to destination address
   *
   * @param ctx
   * @param msg
   */
  private void handleTransferMessage(ChannelHandlerContext ctx, LanMessage msg) throws IOException,
      InvalidAlgorithmParameterException {
    ICrypt crypt = ctx.channel().attr(ATTR_CRYPT_KEY).get();
    ByteBuf decryptBuf = crypt.decrypt(ByteStrUtils.getFixedLenHeapBuf(msg.getData()));

    Channel outRelayChannel = ctx.channel().attr(ATTR_OUT_RELAY_CHANNEL_REF).get().get();
    outRelayChannel.writeAndFlush(decryptBuf);
  }

  private void handleHeartbeatMessage(ChannelHandlerContext ctx, LanMessage beatMsg) {
    Long beatNo = ctx.channel().attr(ATTR_LAST_BEAT_NO).get();
    if (beatNo == null || !beatMsg.getSequenceNum().equals(beatNo)) {
      logger.warn("[ {} ] heart beat number wrong: {}!={}", ctx.channel().id(), beatMsg.getSequenceNum(), beatNo);
    }
  }

  /**
   * Need to build two channel: destination address & lan server. After build success, bind to each other.
   * then send CONN reply msg to lan server withe requestId.
   *
   * @param ctx
   * @param connMsg
   * @throws InvalidAlgorithmParameterException
   * @throws ConnectException
   */
  private void handleConnectionMessage(ChannelHandlerContext ctx, LanMessage connMsg)
      throws InvalidAlgorithmParameterException, ConnectException{

    String requestId = connMsg.getRequestId();

    String uri = connMsg.getUri();
    if (StringUtils.isEmpty(uri) || uri.split(COLON).length < 2) {
      logger.warn("[ {} ] destination address is wrong: {} => {}", ctx.channel(), requestId, uri);
      throw new ConnectException(requestId);
    }

    ICrypt crypt = CryptFactory.get(Config.METHOD, Config.PASSWORD);

    final AtomicReference<Channel> outRelayChannelRef = new AtomicReference<>();
    final AtomicReference<Channel> inRelayChannelRef = new AtomicReference<>();

    String[] dst = uri.split(COLON);
    String dstAddr = dst[0];
    Integer dstPort = Integer.valueOf(dst[1]);

    // connect to destination address
    new Bootstrap().group(ctx.channel().eventLoop())
        .channel(Constants.channelClass)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT)
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .handler(OutBoundInitializer.INSTANCE)
        .connect(dstAddr, dstPort)
        .addListener((ChannelFutureListener) future -> {
          synchronized (connMsg) {
            if (future.isSuccess()) {
              outRelayChannelRef.set(future.channel());
              if (inRelayChannelRef.get() != null) {
                bindAndReply(requestId, outRelayChannelRef, inRelayChannelRef);
              }
            } else {
              throw new ConnectException("connect failed: " + future.cause().getMessage());
            }
          }
        });

    // connect to lan server
    new Bootstrap().group(LanClientMain.workerGroup)
        .channel(Constants.channelClass)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT)
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .handler(InBoundInitializer.INSTANCE)
        .connect(Config.LAN_SERVER_HOST, Config.LAN_SERVER_PORT)
        .addListener((ChannelFutureListener) future -> {
          synchronized (connMsg) {
            if (future.isSuccess()) {
              Channel inRelayChannel = future.channel();
              inRelayChannel.attr(ATTR_CRYPT_KEY).set(crypt);
              inRelayChannelRef.set(inRelayChannel);
              if (outRelayChannelRef.get() != null) {
                bindAndReply(requestId, outRelayChannelRef, inRelayChannelRef);
              }
            } else {
              throw new ConnectException("connect failed: " + future.cause().getMessage());
            }
          }
        });
  }

  /**
   * bind to each other and reply to lan server that, all two channels are built success
   *
   * @param requestId
   * @param outChRef
   * @param inChRef
   */
  private void bindAndReply(String requestId, AtomicReference<Channel> outChRef, AtomicReference<Channel> inChRef) {
    inChRef.get().attr(ATTR_OUT_RELAY_CHANNEL_REF).set(outChRef);
    outChRef.get().attr(ATTR_IN_RELAY_CHANNEL).set(inChRef.get());

    LanMessage lanMessage = LanMsgUtils.packConnectedMsg(requestId);
    inChRef.get().writeAndFlush(lanMessage);
  }

  /**
   * reConnect when inActive
   *
   * @param ctx
   * @throws Exception
   */
  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    super.channelInactive(ctx);
    // only main channel need to reconnect
    if (ctx.channel().attr(IS_MAIN_CHANNEL).get()) {
      LanClientMain.doConnect();
    }
  }
}
