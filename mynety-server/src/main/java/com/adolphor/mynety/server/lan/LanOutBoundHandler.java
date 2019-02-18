package com.adolphor.mynety.server.lan;

import com.adolphor.mynety.common.bean.lan.LanMessage;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.utils.ByteStrUtils;
import com.adolphor.mynety.common.utils.ChannelUtils;
import com.adolphor.mynety.common.utils.LanMsgUtils;
import com.adolphor.mynety.common.wrapper.AbstractSimpleHandler;
import com.adolphor.mynety.server.lan.utils.LanChannelContainers;
import com.adolphor.mynety.server.lan.utils.LanClient;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

import static com.adolphor.mynety.common.constants.Constants.ATTR_CONNECTED_TIMESTAMP;
import static com.adolphor.mynety.common.constants.Constants.ATTR_CRYPT_KEY;
import static com.adolphor.mynety.common.constants.Constants.ATTR_IN_RELAY_CHANNEL;
import static com.adolphor.mynety.common.constants.Constants.ATTR_OUT_RELAY_CHANNEL_REF;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_TEMP_MSG;
import static com.adolphor.mynety.common.constants.LanConstants.ATTR_LOST_BEAT_CNT;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
@ChannelHandler.Sharable
public class LanOutBoundHandler extends AbstractSimpleHandler<LanMessage> {

  public static final LanOutBoundHandler INSTANCE = new LanOutBoundHandler();

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, LanMessage msg) throws Exception {
    switch (msg.getType()) {
      case CLIENT:
        handleClientMessage(ctx);
        break;
      case CONNECTED:
        handleConnectedMessage(ctx, msg);
        break;
      case HEARTBEAT:
        handleHeartbeatMessage(ctx, msg);
        break;
      case TRANSMIT:
        handleTransferMessage(ctx, msg);
        break;
      default:
        throw new IllegalArgumentException("NOT supported msg type:" + msg.getType());
    }
  }

  /**
   * main channel of lan client
   *
   * @param ctx
   */
  private void handleClientMessage(ChannelHandlerContext ctx) {
    LanChannelContainers.clientMainChannel = ctx.channel();
  }

  /**
   * connection reply msg:
   * 1. bind inRelay and outRelay to each other by requestId;
   * 2. consume cache msg
   *
   * @param ctx
   * @param msg
   */
  private void handleConnectedMessage(ChannelHandlerContext ctx, LanMessage msg) throws Exception {
    String requestId = msg.getRequestId();
    LanClient lanClient = LanChannelContainers.lanMaps.get(requestId);
    Channel inRelayChannel = lanClient.getInRelayChannel();

    AtomicReference<Channel> outRelayChannelRef = inRelayChannel.attr(ATTR_OUT_RELAY_CHANNEL_REF).get();
    outRelayChannelRef.set(ctx.channel());
    ctx.channel().attr(ATTR_IN_RELAY_CHANNEL).set(inRelayChannel);

    ICrypt lanCrypt = lanClient.getCrypt();
    ctx.channel().attr(ATTR_CRYPT_KEY).set(lanCrypt);

    AtomicReference tempMstRef = inRelayChannel.attr(ATTR_REQUEST_TEMP_MSG).get();
    if (tempMstRef.get() != null) {
      synchronized (outRelayChannelRef) {
        byte[] data = lanCrypt.encryptToArray((ByteBuf) tempMstRef.get());
        LanMessage lanMessage = LanMsgUtils.packTransferMsg(data);
        ctx.channel().writeAndFlush(lanMessage);
      }
    }

  }

  /**
   * from socks server to lan client
   *
   * @param ctx
   * @param msg
   */
  private void handleTransferMessage(ChannelHandlerContext ctx, LanMessage msg) throws Exception {

    Channel inRelayChannel = ctx.channel().attr(ATTR_IN_RELAY_CHANNEL).get();
    ICrypt inRelayCrypt = inRelayChannel.attr(ATTR_CRYPT_KEY).get();
    ICrypt lanCrypt = ctx.channel().attr(ATTR_CRYPT_KEY).get();

    byte[] data = msg.getData();

    ByteBuf decryptBuf = lanCrypt.decrypt(ByteStrUtils.getFixedLenHeapBuf(data));
    ByteBuf encryptBuf = inRelayCrypt.encrypt(decryptBuf);

    inRelayChannel.writeAndFlush(encryptBuf);
  }

  /**
   * @param ctx
   * @param msg
   */
  private void handleHeartbeatMessage(ChannelHandlerContext ctx, LanMessage msg) {
    ctx.channel().attr(ATTR_LOST_BEAT_CNT).set(0L);
    ctx.channel().writeAndFlush(msg);
  }

  @Override
  protected void channelClose(ChannelHandlerContext ctx) {
    Channel inRelayChannel = ctx.channel().attr(ATTR_IN_RELAY_CHANNEL).get();
    if (inRelayChannel != null && inRelayChannel.isActive()) {
      long connTime = System.currentTimeMillis() - ctx.channel().attr(ATTR_CONNECTED_TIMESTAMP).get();
      ChannelUtils.closeOnFlush(inRelayChannel);
    }
    super.channelClose(ctx);
  }


}