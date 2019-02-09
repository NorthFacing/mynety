package com.adolphor.mynety.client.http;

import com.adolphor.mynety.common.bean.Address;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.utils.ChannelUtils;
import com.adolphor.mynety.common.utils.DomainUtils;
import com.adolphor.mynety.common.wrapper.AbstractInBoundHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.ATTR_IS_HTTP_TUNNEL;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_ADDRESS;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_TEMP_MSG;
import static com.adolphor.mynety.common.constants.Constants.LOG_MSG;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * http 代理入口，接收的对象肯定是 FullHttpRequest 类型，当前实现不加权限验证
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.4
 */
@Slf4j
@ChannelHandler.Sharable
public class HttpProxyHandler extends AbstractInBoundHandler<FullHttpRequest> {

  public static final HttpProxyHandler INSTANCE = new HttpProxyHandler();

  /**
   * 处理 HTTP 请求的第一条消息，其中最重要的一个功能就是解析出当前请求的目标地址并缓存。
   * 目前保持handler和HttpInBoundHandler分开处理，这样HttpInBoundHandler的channelRead0中不需要处理建立连接的逻辑
   *
   * @param ctx
   * @param msg
   * @throws Exception
   */
  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest msg) throws Exception {

    // 使用apache2结合postman没有测试出哑代理的问题，后续跟踪TCP握手协议
    logger.debug("[ {}{} ]【HTTP请求分发】第一条 http 消息: {}", ctx.channel().id(), LOG_MSG, msg);
    HttpHeaders headers = msg.headers();
    if (headers.size() > 0 ){
      if ("keep-alive".equalsIgnoreCase(headers.get(HttpHeaderNames.PROXY_CONNECTION))){
        headers.set(HttpHeaderNames.CONNECTION,"keep-alive");
      }
    }

    Address address = DomainUtils.getAddress(msg.uri());
    logger.debug("[ {}{} ]【{}】HTTP请求分发 解析URL信息 {} => {}:{}", ctx.channel().id(), LOG_MSG, getSimpleName(this), msg.uri(), address.getHost(), address.getPort());
    ctx.channel().attr(ATTR_REQUEST_ADDRESS).set(address);

    // 判断是否是tunnel代理，作为后续是否移除 httpCodec 处理器的标准之一（另外一个标准是 是否开启MITM）
    if (HttpMethod.CONNECT == msg.method()) {
      ctx.channel().attr(ATTR_IS_HTTP_TUNNEL).set(true);
      logger.info("[ {} ]【{}】HTTP请求分发 添加 ATTR_IS_HTTP_TUNNEL 属性：true", ctx.channel().id(), getSimpleName(this));
    } else {
      // 如果不是tunnel代理，HTTP客户端不发送第二次请求，所以需要将本次请求进行缓存
      ctx.channel().attr(ATTR_IS_HTTP_TUNNEL).set(false);
      logger.info("[ {} ]【{}】HTTP请求分发 添加 ATTR_IS_HTTP_TUNNEL 属性：false", ctx.channel().id(), getSimpleName(this));
      ReferenceCountUtil.retain(msg);
      ctx.channel().attr(ATTR_REQUEST_TEMP_MSG).get().set(msg);
      logger.debug("[ {}{} ]【{}】HTTP请求分发 暂存消息到缓存...", ctx.channel().id(), Constants.LOG_MSG, getSimpleName(this));
    }
    ctx.pipeline().addLast(HttpInBoundHandler.INSTANCE);
    ctx.pipeline().remove(this);
    logger.info("[ {} ]【{}】HTTP请求分发 移除处理器: HttpProxyHandler", ctx.channel().id(), getSimpleName(this));
    ctx.fireChannelActive();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ChannelUtils.closeOnFlush(ctx.channel());
    logger.error("[ " + ctx.channel().id() + LOG_MSG + "] error: ", cause);
  }

}
