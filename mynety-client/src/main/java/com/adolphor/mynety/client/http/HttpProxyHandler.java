package com.adolphor.mynety.client.http;

import com.adolphor.mynety.client.http.http_1_0.Http_1_0_ConnectionHandler;
import com.adolphor.mynety.client.http.http_1_1.Http_1_1_ConnectionHandler;
import com.adolphor.mynety.client.http.tunnel.HttpTunnelConnectionHandler;
import com.adolphor.mynety.common.wrapper.AbstractSimpleHandler;
import com.adolphor.mynety.common.utils.SocksServerUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.ATTR_IS_KEEP_ALIVE;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * http 代理入口 请求分发，当前实现不加权限验证
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
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

      // 根据头文件判断请求是否是长连接
      HttpHeaders headers = httpRequest.headers();
      if (headers.size() > 0 && (
          HttpHeaderNames.CONNECTION.contentEqualsIgnoreCase(headers.get(HttpHeaderNames.CONNECTION))
              || HttpHeaderNames.CONNECTION.contentEqualsIgnoreCase(headers.get(HttpHeaderNames.PROXY_CONNECTION))
              || HttpHeaderNames.KEEP_ALIVE.contentEqualsIgnoreCase(headers.get(HttpHeaderNames.PROXY_CONNECTION)))) {
        HttpUtil.setKeepAlive(httpRequest, true);
        ctx.channel().attr(ATTR_IS_KEEP_ALIVE).set(true);
      }

      // 优先判断是否是tunnel代理，HTTP1.0，HTTP1.1，HTTP2.0 都支持（协议规则是否完全一致需要确认）
      if (HttpMethod.CONNECT == httpRequest.method()) {
        ctx.pipeline().addAfter(ctx.name(), null, new HttpTunnelConnectionHandler(httpRequest));
        logger.info("[ {}{} ] choose and add handler by protocol type of http msg: HttpTunnelConnectionHandler", ctx.channel(), LOG_MSG);
      }
      // HTTP1.1
      else if (HTTP_1_1 == httpVersion) {
        ctx.pipeline().addAfter(ctx.name(), null, new Http_1_1_ConnectionHandler(httpRequest));
        logger.info("[ {}{} ] choose and add handler by protocol type of http msg: Http_1_1_ConnectionHandler", ctx.channel(), LOG_MSG);
      }
      // HTTP1.0
      else if (HTTP_1_0 == httpVersion) {
        // 如果是1.0，则去掉connection头部，防止哑代理 (待验证之后放出此逻辑)
        // headers.remove(HttpHeaderNames.CONNECTION);
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
    SocksServerUtils.closeOnFlush(ctx.channel());
    logger.error("[ " + ctx.channel() + LOG_MSG + "] error: ", cause);
  }

}
