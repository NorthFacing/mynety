package com.adolphor.mynety.lan;

import com.adolphor.mynety.common.bean.lan.LanMessage;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.constants.LanMsgType;
import com.adolphor.mynety.common.encryption.CryptFactory;
import com.adolphor.mynety.common.encryption.CryptUtil;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.utils.LanMsgUtils;
import com.adolphor.mynety.common.wrapper.AbstractInBoundHandler;
import com.adolphor.mynety.lan.config.Config;
import com.adolphor.mynety.lan.utils.ChannelContainer;
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
import org.apache.commons.lang3.StringUtils;

import static com.adolphor.mynety.common.constants.Constants.ATTR_IN_RELAY_CHANNEL;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG;
import static com.adolphor.mynety.common.constants.LanConstants.ATTR_LAST_BEAT_NO;
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
public class LanInBoundHandler extends AbstractInBoundHandler<LanMessage> {

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
  protected void channelRead0(ChannelHandlerContext ctx, LanMessage msg) throws Exception {
    logger.info("[ {} ]【{}-{}】收到 ss server 请求信息: {}", ctx.channel().id(), getSimpleName(this), msg.getRequestId(), msg);
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
        logger.info("[ {} ]【{}】不支持的消息类型: {}", ctx.channel(), msg);
        break;
    }
  }

  /**
   * 不是心跳就要进行数据传输：将数据传输至目标服务器或加入缓存
   *
   * @param ctx
   * @param msg 数据请求信息
   */
  private void handleTransferMessage(ChannelHandlerContext ctx, LanMessage msg) {

    // 一个 LAN channel 与多个目标地址关联，所以要 找出/维护 请求对应的 channel
    String requestId = msg.getRequestId();

    ByteBuf decryptBuf;
    byte[] temp;
    if (msg.getData() != null) {
      ICrypt crypt = ChannelContainer.getCrypt(requestId);
      logger.info("[ {} ]【{}-{}】获取编解码器： {}", ctx.channel().id(), getSimpleName(this), requestId, crypt);
      // 这是消息发送之前的转换方式的逆操作
      // 解密方法中只支持"直接缓冲区"数据，所以要进行如下形式的封装（加解密方法后续优化）
      byte[] data = msg.getData();
      ByteBuf buffer = Unpooled.directBuffer().writeBytes(data);
      logger.debug("[ {} ]【{}-{}】解密前的data数据: {} bytes => {}", ctx.channel().id(), getSimpleName(this), msg.getRequestId(), data.length, data);
      temp = CryptUtil.decrypt(crypt, buffer);
      if (temp == null) {
        logger.warn("[ {} ]【{}-{}】信息解密错误...", ctx.channel(), getSimpleName(this));
        return;
      }
      logger.debug("[ {} ]【{}-{}】解密后的data数据: {} bytes => {}", ctx.channel().id(), getSimpleName(this), msg.getRequestId(), temp.length, temp);
      decryptBuf = Unpooled.wrappedBuffer(temp);
    } else {
      logger.debug("[ {} ]【{}-{}】请求信息为空，丢弃处理……", ctx.channel().id(), getSimpleName(this), msg.getRequestId());
      return;
    }

    Channel outRelayChannel = ChannelContainer.getOutRelayChannelRef(requestId).get();
    if (outRelayChannel != null) {
      outRelayChannel.writeAndFlush(decryptBuf);
      logger.debug("[ {}{}{} ]【{}-{}】将信息写入远程目的地址: {} bytes => {}", ctx.channel().id(), LOG_MSG, outRelayChannel.id(), getSimpleName(this), msg.getRequestId(), temp.length, temp);
    } else {
      ByteBuf tempMsg = ChannelContainer.getTempMsgRef(requestId).get();
      tempMsg.writeBytes(decryptBuf);
      logger.debug("[ {} ]【{}-{}】将信息加入缓存: {} bytes => {}", ctx.channel().id(), getSimpleName(this), msg.getRequestId(), temp.length, temp);
    }

  }

  private void handleConnectionMessage(ChannelHandlerContext ctx, LanMessage connMsg) {
    String requestId = connMsg.getRequestId();

    String uri = connMsg.getUri();
    if (StringUtils.isEmpty(uri)) {
      logger.info("[ {} ]【{}-{}】远程请求地址不能为空...", ctx.channel().id(), getSimpleName(this), requestId);
      channelClose(ctx);
    }

    String[] dst = uri.split(":");
    if (dst.length < 2) {
      logger.error("[ {} ]【{}-{}】远程请求地址解析错误：{}", ctx.channel().id(), getSimpleName(this), requestId, uri);
      LanMessage lanMessage = LanMsgUtils.packageLanMsg(ctx.channel(), requestId, LanMsgType.DISCONNECT.getVal());
      ctx.channel().writeAndFlush(lanMessage);
      ChannelContainer.removeReqChannel(requestId);
      logger.info("[ {} ]【{}-{}】发送断开连接信息给lan服务端，需要断开的requestId： {}", ctx.channel().id(), getSimpleName(this), requestId, requestId);
    }

    ICrypt crypt = CryptFactory.get(Config.METHOD, Config.PASSWORD);
    logger.info("[ {} ]【{}-{}】初始化编解码器： {}", ctx.channel().id(), getSimpleName(this), requestId, crypt);
    ChannelContainer.initChannelConfig(requestId, crypt);

    String dstAddr = dst[0];
    Integer dstPort = Integer.valueOf(dst[1]);
    Bootstrap outRelayBootStrap = new Bootstrap();
    outRelayBootStrap.group(ctx.channel().eventLoop())
        .channel(Constants.channelClass)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast(new LanOutBoundHandler(ctx.channel(), requestId, crypt));
            logger.info("[ {}{}{} ]【{}-{}】新增处理器：LanOutBoundHandler", ctx.channel().id(), LOG_MSG, ch, getSimpleName(LanInBoundHandler.this), requestId);
          }
        });
    // 建立连接
    try {
      ChannelFuture channelFuture = outRelayBootStrap.connect(dstAddr, dstPort);
      channelFuture.addListener((ChannelFutureListener) future -> {
        if (future.isSuccess()) {
          Channel outRelayChannel = future.channel();
          // 常规情况下，在远程连接成功之后，需要将 inRelay 和 outRelay互相绑定，但在Lan客户端中，inRelay只有一个，
          // 所以只需要在outRelay中绑定inRelay，在inRelay中通过requestId找到outRelay
          ChannelContainer.getOutRelayChannelRef(requestId).set(outRelayChannel);
          outRelayChannel.attr(ATTR_IN_RELAY_CHANNEL).set(ctx.channel());
          logger.debug("[ {} ]【{}-{}】连接目的地址成功 => {}:{}", ctx.channel().id(), getSimpleName(this), requestId, dstAddr, dstPort);
        } else {
          logger.error("connection error: ", future.cause());
          logger.warn("[ {} ]【{}-{}】连接目的失败 => {}:{}", ctx.channel().id(), getSimpleName(this), requestId, dstAddr, dstPort);
        }
      });

    } catch (Exception e) {
      logger.error(LOG_MSG + "connect internet error", e);
      LanMessage lanMessage = LanMsgUtils.packageLanMsg(ctx.channel(), requestId, LanMsgType.DISCONNECT.getVal());
      ctx.channel().writeAndFlush(lanMessage);
      ChannelContainer.removeReqChannel(requestId);
      logger.info("[ {} ]【{}-{}】发送断开连接信息给lan服务端，需要断开的requestId： {}", ctx.channel().id(), getSimpleName(this), requestId);
    }
  }

  /**
   * 服务端返回的信条消息，需要验证心跳信息是否一致
   *
   * @param ctx
   * @param beatMsg 心跳信息
   */
  private void handleHeartbeatMessage(ChannelHandlerContext ctx, LanMessage beatMsg) {
    logger.info("[ {} ]【{}】处理心跳信息: {}", ctx.channel().id(), getSimpleName(this), beatMsg);
    Long beatNo = ctx.channel().attr(ATTR_LAST_BEAT_NO).get();
    if (beatNo == null || beatMsg.getSerialNumber() != beatNo) {
      logger.error("[ {} ]【{}】心跳序号错误: {}!={}", ctx.channel().id(), getSimpleName(this), beatMsg.getSerialNumber(), beatNo);
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
