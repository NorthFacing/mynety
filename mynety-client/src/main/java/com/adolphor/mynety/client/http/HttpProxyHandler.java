package com.adolphor.mynety.client.http;

import com.adolphor.mynety.common.bean.Address;
import com.adolphor.mynety.common.utils.DomainUtils;
import com.adolphor.mynety.common.wrapper.AbstractInBoundHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.Constants.ATTR_IS_HTTP_TUNNEL;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_ADDRESS;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_TEMP_MSG;
import static com.adolphor.mynety.common.constants.HandlerName.httpInBoundHandler;
import static com.adolphor.mynety.common.constants.HandlerName.httpProxyHandler;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * entrance of http request
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
   * handle the first request of http(s), the main motivation for this handle is to parse the request address,
   * then method {@link HttpInBoundHandler#channelActive} could build the remote
   * connection, and method {@link HttpInBoundHandler#channelRead0} only need to
   * cares about transmit the request msg:
   * <p>
   * 1. if is CONNECT request, it must be tunnel proxy, and then add the tunnel attribute markup
   * 2. if not, cache the request
   *
   * @param ctx
   * @param msg
   * @throws Exception
   */
  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest msg) throws Exception {

    Address address = DomainUtils.getAddress(msg.uri());
    logger.debug("[ {} ] {} request url => {} => {}:{}", ctx.channel().id(), getSimpleName(this), msg.uri(), address.getHost(), address.getPort());
    ctx.channel().attr(ATTR_REQUEST_ADDRESS).set(address);

    if (HttpMethod.CONNECT == msg.method()) {
      ctx.channel().attr(ATTR_IS_HTTP_TUNNEL).set(true);
    } else {
      ctx.channel().attr(ATTR_IS_HTTP_TUNNEL).set(false);
      ReferenceCountUtil.retain(msg);
      ctx.channel().attr(ATTR_REQUEST_TEMP_MSG).get().set(msg);
    }
    ctx.pipeline().addLast(httpInBoundHandler, HttpInBoundHandler.INSTANCE);
    ctx.pipeline().remove(httpProxyHandler);
    ctx.fireChannelActive();
  }

}
