package com.adolphor.mynety.client.socks;

import com.adolphor.mynety.common.constants.Constants;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Socks04DataHandler extends SimpleChannelInboundHandler {

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.info(Constants.LOG_MSG_OUT + ctx.channel() + "【数据】处理器激活，发送真实请求...");
    super.channelActive(ctx);
    byte[] msg = getHttpRequest().getBytes("UTF-8");
    ctx.writeAndFlush(Unpooled.wrappedBuffer(msg));
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    logger.info(Constants.LOG_MSG_OUT + ctx.channel() + "【数据】处理器收到响应消息：{}", msg);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    logger.info(Constants.LOG_MSG_OUT + ctx.channel() + "【数据】处理器数据处理完毕");
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
    logger.error(Constants.LOG_MSG_OUT + ctx.channel() + "【数据】处理器异常：", throwable);
    ctx.channel().close();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    logger.info(Constants.LOG_MSG_OUT + ctx.channel() + "【数据】处理器连接断开：" + ctx.channel());
    super.channelInactive(ctx);
  }

  private static String getHttpRequest() {
    StringBuffer sb = new StringBuffer("GET http://adolphor.com/ HTTP/1.1\r\n")
        .append("Host: adolphor.com\r\n")
        .append("Proxy-Connection: keep-alive\r\n")
        .append("Cache-Control: max-age=0\r\n")
        .append("Upgrade-Insecure-Requests: 1\r\n")
        .append("User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_4) \r\n")
        .append("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8\r\n")
        .append("Accept-Encoding: gzip, deflate\r\n")
        .append("Accept-Language: zh-CN,zh;q=0.9,en;q=0.8,zh-TW;q=0.7\r\n")
        .append("Cookie: _ga=GA1.2.1951019928.1515000515; _gid=GA1.2.11823253.1525229142; _gat=1\r\n")
        .append("If-Modified-Since: Sun, 29 Apr 2018 15:18:33 GMT\r\n");
    return sb.toString();
  }

}
