package com.shadowsocks.client.socks;

import com.shadowsocks.common.constants.Constants;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;

import static com.shadowsocks.client.socks.SocksClientMainTest.DST_HOST;
import static com.shadowsocks.client.socks.SocksClientMainTest.DST_PORT;
import static com.shadowsocks.client.socks.SocksClientMainTest.DST_PROTOCOL;

@Slf4j
public class Socks04DataHandler extends SimpleChannelInboundHandler {

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    log.info(Constants.LOG_MSG + ctx.channel() + "【数据】处理器激活...");
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    log.info(Constants.LOG_MSG + ctx.channel() + "【数据】处理器收到消息：{}", msg);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    log.info(Constants.LOG_MSG + ctx.channel() + "【数据】处理器数据处理完毕，发送新的网页请求");
    ctx.channel().writeAndFlush(getHttpRequest());
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
    log.error(Constants.LOG_MSG + ctx.channel() + "【数据】处理器异常：", throwable);
    ctx.channel().close();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    log.info(Constants.LOG_MSG + ctx.channel() + "【数据】处理器连接断开：" + ctx.channel());
    super.channelInactive(ctx);
  }

  private static HttpRequest getHttpRequest() throws URISyntaxException {
    URI uri = new URI(DST_PROTOCOL + "://" + DST_HOST + ":" + DST_PORT);
    String msg = "Are you ok?";
    DefaultFullHttpRequest request = new DefaultFullHttpRequest(
        HttpVersion.HTTP_1_1,
        HttpMethod.POST,
        uri.toASCIIString(),
        Unpooled.wrappedBuffer(msg.getBytes())
    );
    // 构建http请求
    request.headers().set(HttpHeaderNames.HOST, DST_HOST);
    request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderNames.CONNECTION);
    request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
    request.headers().set("messageType", "normal");
    request.headers().set("businessType", "testServerState");
    return request;
  }

}
