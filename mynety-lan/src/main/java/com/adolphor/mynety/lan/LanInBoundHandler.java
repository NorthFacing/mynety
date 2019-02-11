package com.adolphor.mynety.lan;

import com.adolphor.mynety.common.bean.lan.LanMessage;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.constants.LanMsgType;
import com.adolphor.mynety.common.encryption.CryptFactory;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.utils.ByteStrUtils;
import com.adolphor.mynety.common.utils.LanMsgUtils;
import com.adolphor.mynety.common.wrapper.AbstractSimpleHandler;
import com.adolphor.mynety.lan.config.Config;
import com.adolphor.mynety.lan.utils.ChannelContainer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.ConnectException;
import java.security.InvalidAlgorithmParameterException;

import static com.adolphor.mynety.common.constants.Constants.ATTR_IN_RELAY_CHANNEL;
import static com.adolphor.mynety.common.constants.Constants.COLON;
import static com.adolphor.mynety.common.constants.Constants.CONNECT_TIMEOUT;
import static com.adolphor.mynety.common.constants.LanConstants.ATTR_LAST_BEAT_NO;
import static org.apache.commons.lang3.ClassUtils.getName;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * lan客户端与lan服务器的链接
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
@ChannelHandler.Sharable
public class LanInBoundHandler extends AbstractSimpleHandler<LanMessage> {

  public static final LanInBoundHandler INSTANCE = new LanInBoundHandler();

  /**
   * -- 1. 第一次请求 LAN_MSG_CONNECT
   * ------ 1.1 则将请求信息加入到缓存列表
   * ------ 1.2 创建远程连接并将channel保存在缓存中
   * -- 2. 非第一次请求 LAN_MSG_TRANSFER
   * ------ 2.1 目的连接尚未成功，将请求数据放入缓存列表
   * ------ 2.2 已经成功，直接用缓存中获取的channel进行数据传输
   *
   * @param ctx
   * @param msg
   * @throws Exception
   */
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, LanMessage msg) throws InvalidAlgorithmParameterException, IOException {
    switch (msg.getType()) {
      case HEARTBEAT:
        handleHeartbeatMessage(ctx, msg);
        break;
      case CONNECT:
        handleConnectionMessage(ctx, msg);
        break;
      case TRANSFER:
        handleTransferMessage(ctx, msg);
        break;
      default:
        logger.info("[ {} ] {} unsupported msg type: {}", ctx.channel(), msg);
        break;
    }
  }

  /**
   * 不是心跳就要进行数据传输：将数据传输至目标服务器或加入缓存
   *
   * @param ctx
   * @param msg 数据请求信息
   */
  private void handleTransferMessage(ChannelHandlerContext ctx, LanMessage msg) throws IOException, InvalidAlgorithmParameterException {

    String requestId = msg.getRequestId();

    ByteBuf decryptBuf;
    if (msg.getData() != null) {
      ICrypt crypt = ChannelContainer.getCrypt(requestId);
      byte[] data = msg.getData();
      ByteBuf buffer = ByteStrUtils.getHeapBuf(data);
      decryptBuf = crypt.decrypt(buffer);
      if (decryptBuf == null || decryptBuf.readableBytes() == 0) {
        return;
      }
    } else {
      logger.warn("[ {} ] {}-{} msg content is empty......", ctx.channel().id(), getSimpleName(this), msg.getRequestId());
      return;
    }

    Channel outRelayChannel = ChannelContainer.getOutRelayChannelRef(requestId).get();
    if (outRelayChannel != null) {
      outRelayChannel.writeAndFlush(decryptBuf);
    } else {
      ByteBuf tempMsg = ChannelContainer.getTempMsgRef(requestId).get();
      tempMsg.writeBytes(decryptBuf);
    }

  }

  private void handleConnectionMessage(ChannelHandlerContext ctx, LanMessage connMsg) throws InvalidAlgorithmParameterException, ConnectException {
    String requestId = connMsg.getRequestId();

    String uri = connMsg.getUri();
    if (StringUtils.isEmpty(uri) || uri.split(COLON).length < 2) {
      logger.warn("[ {} ] {}-{} remote address is wrong: {}", ctx.channel().id(), getSimpleName(this), requestId, uri);
      throw new ConnectException(requestId);
    }

    ICrypt crypt = CryptFactory.get(Config.METHOD, Config.PASSWORD);
    ChannelContainer.initChannelConfig(requestId, crypt);

    String[] dst = uri.split(COLON);
    String dstAddr = dst[0];
    Integer dstPort = Integer.valueOf(dst[1]);
    Bootstrap outRelayBootStrap = new Bootstrap();
    outRelayBootStrap.group(ctx.channel().eventLoop())
        .channel(Constants.channelClass)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT)
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline()
                .addLast(new LoggingHandler(LogLevel.DEBUG))
                .addLast(new LanOutBoundHandler(ctx.channel(), requestId, crypt));
          }
        });
    outRelayBootStrap.connect(dstAddr, dstPort).addListener((ChannelFutureListener) future -> {
      if (future.isSuccess()) {
        Channel outRelayChannel = future.channel();
        ChannelContainer.getOutRelayChannelRef(requestId).set(outRelayChannel);
        outRelayChannel.attr(ATTR_IN_RELAY_CHANNEL).set(ctx.channel());
      } else {
        logger.warn("[ {} ] {}-{} connect remote address failed: {}", ctx.channel().id(), getSimpleName(this), requestId, uri);
        logger.debug(ctx.channel().id() + " => " + future.cause().getMessage(), future.cause());
        throw new ConnectException(requestId);
      }
    });
  }

  /**
   * 服务端返回的信条消息，需要验证心跳信息是否一致
   *
   * @param ctx
   * @param beatMsg 心跳信息
   */
  private void handleHeartbeatMessage(ChannelHandlerContext ctx, LanMessage beatMsg) {
    Long beatNo = ctx.channel().attr(ATTR_LAST_BEAT_NO).get();
    if (beatNo == null || !beatMsg.getSequenceNumber().equals(beatNo)) {
      logger.warn("[ {} ] {} heart beat number wrong: {}!={}", ctx.channel().id(), getSimpleName(this), beatMsg.getSequenceNumber(), beatNo);
    }
  }

  /**
   * 连接断开的时候要重新登录：
   * 1. 心跳超时主动断开
   * 2. 服务器重启等状况导致的连接断开
   *
   * @param ctx
   * @throws Exception
   */
  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    super.channelInactive(ctx);
    LanClientMain.doConnect();
  }

  /**
   * in general, when get an exception, the channel will be closed.
   * but at here, only send disconnection msg to socks server
   *
   * @param ctx
   * @param cause
   */
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    if (cause instanceof ConnectException) {
      String requestId = cause.getMessage();
      LanMessage lanMessage = LanMsgUtils.packageLanMsg(ctx.channel(), requestId, LanMsgType.DISCONNECT);
      ctx.channel().writeAndFlush(lanMessage);
      ChannelContainer.removeReqChannel(requestId);
      logger.warn("[ {} ] {}-{} send disconnect msg to socks server...", ctx.channel().id(), getSimpleName(this), requestId);
    } else if (cause instanceof InvalidAlgorithmParameterException) {
      logger.warn("[ " + ctx.channel().id() + " ] [" + getName(this) + "] InvalidAlgorithmParameterException exception: ", cause);
    } else if (cause instanceof IOException) {
      logger.warn("[ " + ctx.channel().id() + " ] [" + getName(this) + "] IOException exception: ", cause);
    } else {
      logger.warn("[ " + ctx.channel().id() + " ] [" + getName(this) + "] exception: ", cause);
    }
  }

}
