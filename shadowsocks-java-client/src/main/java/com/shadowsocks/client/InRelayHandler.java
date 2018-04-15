package com.shadowsocks.client;

import com.shadowsocks.client.config.ServerConfig;
import com.shadowsocks.common.encryption.ICrypt;
import com.shadowsocks.common.utils.SocksServerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;

/**
 * 接受remoteServer的数据，发送给客户端
 */
public final class InRelayHandler extends ChannelInboundHandlerAdapter {

  private static Logger logger = LoggerFactory.getLogger(InRelayHandler.class);

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    Channel clientChannel = ctx.channel().attr(ServerConfig.CLIENT_CHANNEL).get();
    Boolean isProxy = ctx.channel().attr(ServerConfig.IS_PROXY).get();
    ICrypt crypt = ctx.channel().attr(ServerConfig.CRYPT_KEY).get();

    ByteBuf byteBuf = (ByteBuf) msg;
    try (ByteArrayOutputStream _localOutStream = new ByteArrayOutputStream()) {

      if (clientChannel.isActive()) {
        if (!byteBuf.hasArray()) {
          int len = byteBuf.readableBytes();
          byte[] arr = new byte[len];
          byteBuf.getBytes(0, arr);
          if (isProxy) {
            crypt.decrypt(arr, arr.length, _localOutStream);
            arr = _localOutStream.toByteArray();
          }
          clientChannel.writeAndFlush(Unpooled.wrappedBuffer(arr));
        }
      }
    } catch (Exception e) {
      logger.error(LOG_MSG + ctx.channel() + " Receive remoteServer data error: ", e);
    } finally {
      ReferenceCountUtil.release(msg);
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    Channel clientChannel = ctx.channel().attr(ServerConfig.CLIENT_CHANNEL).get();
    if (clientChannel.isActive()) {
      SocksServerUtils.closeOnFlush(clientChannel);
      SocksServerUtils.closeOnFlush(ctx.channel());
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    logger.error(LOG_MSG + ctx.channel(), cause);
    ctx.close();
  }
}
