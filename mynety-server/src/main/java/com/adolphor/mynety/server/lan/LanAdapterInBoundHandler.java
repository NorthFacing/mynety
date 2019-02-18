package com.adolphor.mynety.server.lan;

import com.adolphor.mynety.common.bean.Address;
import com.adolphor.mynety.common.bean.lan.LanMessage;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.encryption.CryptFactory;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.utils.BaseUtils;
import com.adolphor.mynety.common.utils.LanMsgUtils;
import com.adolphor.mynety.common.wrapper.AbstractSimpleHandler;
import com.adolphor.mynety.server.config.Config;
import com.adolphor.mynety.server.lan.utils.LanChannelContainers;
import com.adolphor.mynety.server.lan.utils.LanClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

import static com.adolphor.mynety.common.constants.Constants.ATTR_CRYPT_KEY;
import static com.adolphor.mynety.common.constants.Constants.ATTR_OUT_RELAY_CHANNEL_REF;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_ADDRESS;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_TEMP_MSG;
import static com.adolphor.mynety.common.constants.Constants.COLON;

/**
 * transmit socks server to lan client
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
@ChannelHandler.Sharable
public final class LanAdapterInBoundHandler extends AbstractSimpleHandler<ByteBuf> {

  public static final LanAdapterInBoundHandler INSTANCE = new LanAdapterInBoundHandler();

  /**
   * 通过 lanMainChannel 发送请求发起新连接指令，并将当前消息缓存到队列
   *
   * @param ctx
   * @throws Exception
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);

    Channel clientMainChannel = LanChannelContainers.clientMainChannel;

    if (clientMainChannel == null || !clientMainChannel.isActive()) {
      throw new IllegalAccessException("lan client is not work ...");
    }

    Channel inRelayChannel = ctx.channel();

    String requestId = BaseUtils.getUUID();
    ICrypt lanCrypt = CryptFactory.get(Config.LAN_METHOD, Config.LAN_PASSWORD);
    LanChannelContainers.lanMaps.put(requestId, new LanClient(inRelayChannel, lanCrypt));

    Address address = inRelayChannel.attr(ATTR_REQUEST_ADDRESS).get();
    String landDstAddr;
    if (Config.LAN_HOST_NAME.equalsIgnoreCase(address.getHost())) {
      landDstAddr = Constants.LOOPBACK_ADDRESS;
    } else {
      landDstAddr = address.getHost();
    }
    String uri = landDstAddr + COLON + address.getPort();
    LanMessage lanMessage = LanMsgUtils.packConnectMsg(requestId, uri);
    clientMainChannel.writeAndFlush(lanMessage).addListener((ChannelFutureListener) future -> {
      if (!future.isSuccess()) {
        throw new Exception(future.cause().getMessage(), future.cause());
      }
    });
  }

  @Override
  @SuppressWarnings("Duplicates")
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    AtomicReference<Channel> outRelayChannelRef = ctx.channel().attr(ATTR_OUT_RELAY_CHANNEL_REF).get();
    ICrypt crypt = ctx.channel().attr(ATTR_CRYPT_KEY).get();

    ByteBuf decryptBuf = crypt.decrypt(msg);

    synchronized (outRelayChannelRef) {
      Channel outRelayChannel = outRelayChannelRef.get();
      if (outRelayChannel != null) {
        ICrypt lanCrypt = outRelayChannel.attr(ATTR_CRYPT_KEY).get();
        byte[] data = lanCrypt.encryptToArray(decryptBuf);
        LanMessage lanMessage = LanMsgUtils.packTransferMsg(data);
        outRelayChannel.writeAndFlush(lanMessage);
        return;
      }

      AtomicReference tempMsgRef = ctx.channel().attr(ATTR_REQUEST_TEMP_MSG).get();
      if (tempMsgRef.get() == null) {
        tempMsgRef.set(decryptBuf);
        return;
      }

      ByteBuf tempBuf = (ByteBuf) tempMsgRef.get();
      if (tempBuf.isWritable()) {
        tempBuf.writeBytes(decryptBuf);
      } else {
        ByteBuf byteBuf = Unpooled.buffer().writeBytes(tempBuf);
        byteBuf.writeBytes(decryptBuf);
        tempMsgRef.set(byteBuf);
      }
    }

  }

}
