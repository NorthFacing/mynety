package com.adolphor.mynety.lan;

import com.adolphor.mynety.common.bean.lan.LanMessage;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.encryption.CryptFactory;
import com.adolphor.mynety.common.encryption.CryptUtil;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.wrapper.AbstractSimpleHandler;
import com.adolphor.mynety.lan.config.Config;
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
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.adolphor.mynety.common.constants.Constants.LOG_MSG;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG_OUT;
import static com.adolphor.mynety.common.constants.LanConstants.ATTR_LAST_BEAT_NO;
import static com.adolphor.mynety.common.constants.LanConstants.LAN_MSG_HEARTBEAT;
import static com.adolphor.mynety.common.constants.LanConstants.LAN_MSG_TRANSFER;

/**
 * TODO lan 建立远程失败的时候要发送断开指令给server，告诉server断开其和用户的连接
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
public class LanConnectionHandler extends AbstractSimpleHandler<LanMessage> {

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.info("[ {}{}{} ] [LanConnectionHandler-channelActive] channel active...", ctx.channel(), LOG_MSG, ctx.channel());
    ctx.channel().attr(Constants.ATTR_CRYPT_KEY).setIfAbsent(CryptFactory.get(Config.METHOD, Config.PASSWORD));
  }

  @Override
  @SuppressWarnings("Duplicates")
  protected void channelRead0(ChannelHandlerContext ctx, LanMessage msg) throws Exception {
    logger.info("[ {} ] [LanConnectionHandler-channelRead0] received msg: {}", ctx.channel(), msg);
    switch (msg.getType()) {
      case LAN_MSG_HEARTBEAT:
        handleHeartbeatMessage(ctx, msg);
        break;
      case LAN_MSG_TRANSFER:
        handleTransferMessage(ctx, msg);
        break;
      default:
        logger.info("[ {} ] [LanConnectionHandler-channelRead0] unsupported msg type: {}", ctx.channel(), msg);
        break;
    }
  }

  /**
   * 数据传输：将数据传输至目标服务器
   *
   * @param ctx
   * @param msg 数据请求信息
   */
  private void handleTransferMessage(ChannelHandlerContext ctx, LanMessage msg) {

    ICrypt crypt = ctx.channel().attr(Constants.ATTR_CRYPT_KEY).get();
    Channel clientChannel = ctx.channel();

    // 一个 LAN channel 与多个目标地址关联，所以要 找出/维护 请求对应的 channel
    String requestId = msg.getRequestId();
    AtomicReference<Channel> remoteChannelRef = ChannelContainer.getRemoteChannelRef(requestId);
    List<ByteBuf> requestTempList = ChannelContainer.getRequestTempList(requestId);

    // 不是心跳就要进行转发
    // -- 1. 判断远程连接是否已经创建
    // ------ 2.1 已经创建成功，直接用缓存中获取的channel进行数据传输
    // ------ 2.2 尚未创建成功，则将请求信息加入到缓存列表
    // -- 1. 第一次需要创建远程连接并将channel保存在缓存中

    // 如果已经建立过连接，则发送消息
    boolean requested = ChannelContainer.isRequested(requestId);
    if (requested) {
      ByteBuf decryptBuf;
      if (msg.getData() != null) {
        // 解密方法中只支持"直接缓冲区"数据，所以要进行如下形式的封装（加解密方法后续优化）
        ByteBuf byteBuf = Unpooled.directBuffer().writeBytes(msg.getData());
        byte[] temp = CryptUtil.decrypt(crypt, byteBuf);
        if (temp == null) {
          logger.warn("[ {}{}{} ] [LanConnectionHandler-channelRead0] handleTransferMessage decrypt msg error...");
          return;
        }
        decryptBuf = Unpooled.wrappedBuffer(temp);
      } else {
        decryptBuf = Unpooled.wrappedBuffer(Unpooled.EMPTY_BUFFER);
      }
      logger.debug("[ {}{} ] [LanConnectionHandler-channelRead0] handleTransferMessage: msg after decrypt: {} bytes => {}",
          ctx.channel(), LOG_MSG, decryptBuf.readableBytes(), decryptBuf);
      Channel remoteChannel = remoteChannelRef.get();
      boolean tempEmpty = ChannelContainer.isTempEmpty(requestId);
      if (remoteChannel != null && tempEmpty) {
        remoteChannel.writeAndFlush(decryptBuf);
        logger.debug("[ {}{}{} ] [LanConnectionHandler-channelRead0] handleTransferMessage: write msg to dst host channel: {} bytes => {}",
            ctx.channel(), LOG_MSG_OUT, remoteChannelRef.get(), decryptBuf.readableBytes(), decryptBuf);
      } else {
        requestTempList.add(decryptBuf);
      }
    }
    // 如果是第一次请求，则创建相应连接
    else {
      // 获取目标地址
      String uri = msg.getUri();
      if (StringUtils.isEmpty(uri)) {
        logger.info("[ {}{}{} ] [LanConnectionHandler-channelActive] handleTransferMessage: msg uri should NOT be null...", ctx.channel(), LOG_MSG);
        channelClose(ctx);
      }

      String[] dst = uri.split(":");
      if (dst.length < 2) {
        logger.error("{}{} [ConnectionHandler-channelActive] lan client parse dst uri failed => {}", LOG_MSG, clientChannel, uri);
        // TODO 发送断开指令到server && 清除缓存信息
      }

      String dstAddr = dst[0];
      Integer dstPort = Integer.valueOf(dst[1]);

      Bootstrap remoteBootStrap = new Bootstrap();

      remoteBootStrap.group(clientChannel.eventLoop())
          .channel(Constants.channelClass)
          .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)
          .handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
              ch.pipeline().addLast(new LanRemoteHandler(ctx.channel(), crypt, requestId, requestTempList));
              logger.info("[ {}{}{} ] [LanConnectionHandler-channelActive] out pipeline add handler: LanRemoteHandler", ctx.channel(), LOG_MSG, ch);
            }
          });

      // 建立连接
      try {
        ChannelFuture channelFuture = remoteBootStrap.connect(dstAddr, dstPort);
        channelFuture.addListener((ChannelFutureListener) future -> {
          if (future.isSuccess()) {
            Channel remoteChannel = future.channel();
            remoteChannelRef.set(remoteChannel);
            logger.debug("{}{} [ConnectionHandler-channelActive] lan client connect to dst host success => {}:{}", LOG_MSG_OUT, clientChannel, dstAddr, dstPort);
          } else {
            logger.error("connection error: ", future.cause());
            logger.warn("{}{} [ConnectionHandler-channelActive] lan client connect to dst host failed => {}:{}", LOG_MSG, clientChannel, dstAddr, dstPort);
          }
        });

      } catch (Exception e) {
        logger.error(LOG_MSG + "connect internet error", e);
        // TODO 发送断开指令到server && 清除缓存信息
      }
    }

  }

  /**
   * 服务端返回的信条消息，需要验证心跳信息是否一致
   *
   * @param ctx
   * @param beatMsg 心跳信息
   */
  private void handleHeartbeatMessage(ChannelHandlerContext ctx, LanMessage beatMsg) {
    logger.info("[ {} ] [LanConnectionHandler-channelRead0] received response-heartbeat message: {}", ctx.channel(), beatMsg);
    Long beatNo = ctx.channel().attr(ATTR_LAST_BEAT_NO).get();

    if (beatNo == null || beatMsg.getSerialNumber() != beatNo) {
      logger.error("[ {} ] [LanConnectionHandler-channelRead0] handleHeartbeatMessage, beatNO: {}!={}", ctx.channel(), beatMsg.getSerialNumber(), beatNo);
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


}
