package com.shadowsocks.client.http;

import com.shadowsocks.client.http.http_1_0.Http_1_0_ConnectionHandler;
import com.shadowsocks.client.http.http_1_1.Http_1_1_ConnectionHandler;
import com.shadowsocks.client.http.tunnel.HttpTunnelConnectionHandler;
import com.shadowsocks.common.nettyWrapper.AbstractSimpleHandler;
import com.shadowsocks.common.utils.SocksServerUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpVersion;
import lombok.extern.slf4j.Slf4j;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;

/**
 * http 代理入口 请求分发，当前实现不加权限验证
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public class HttpProxyHandler extends AbstractSimpleHandler<HttpObject> {

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
    logger.debug("[ {}{} ] http proxy receive first http msg: {}", ctx.channel(), LOG_MSG, msg);
    if (msg instanceof DefaultHttpRequest) {
      DefaultHttpRequest httpRequest = (DefaultHttpRequest) msg;
      HttpVersion httpVersion = httpRequest.protocolVersion();
      // 优先判断是否是tunnel代理，HTTP1.0，HTTP1.1，HTTP2.0 都支持（协议规则是否完全一致需要确认）
      if (HttpMethod.CONNECT == httpRequest.method()) {
        ctx.pipeline().addAfter(ctx.name(), null, new HttpTunnelConnectionHandler(httpRequest));
        logger.info("[ {}{} ] choose and add handler by protocol type of http msg: HttpTunnelConnectionHandler", ctx.channel(), LOG_MSG);
      }
      // HTTP1.1
      else if (HttpMethod.CONNECT == httpRequest.method()) {
        ctx.pipeline().addAfter(ctx.name(), null, new Http_1_1_ConnectionHandler(httpRequest));
        logger.info("[ {}{} ] choose and add handler by protocol type of http msg: Http_1_1_ConnectionHandler", ctx.channel(), LOG_MSG);
      }
      // HTTP1.0
      else if (HttpMethod.CONNECT == httpRequest.method()) {
        ctx.pipeline().addAfter(ctx.name(), null, new Http_1_0_ConnectionHandler(httpRequest));
        logger.info("[ {}{} ] choose and add handler by protocol type of http msg: Http_1_0_ConnectionHandler", ctx.channel(), LOG_MSG);
      }
      // To be done...
      else {
        logger.error("NOT SUPPORTED {} FOR NOW...", httpVersion);
        ctx.close();
      }
      ctx.pipeline().remove(this);
      logger.info("[ {}{} ] remove handler: HttpProxyHandler", ctx.channel(), LOG_MSG);
      ctx.fireChannelActive();
    } else { // 如果第一次请求不是 DefaultHttpRequest 那么就说明HTTP请求异常
      logger.error("[ {}{} ] unhandled msg, type: {}", ctx.channel(), LOG_MSG, msg.getClass().getTypeName());
      channelClose(ctx);
    }

  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    SocksServerUtils.flushOnClose(ctx.channel());
    logger.error("[ " + ctx.channel() + LOG_MSG + "] error: ", cause);
  }

  @Override
  protected void channelClose(ChannelHandlerContext ctx) {
    SocksServerUtils.flushOnClose(ctx.channel());
  }

}
