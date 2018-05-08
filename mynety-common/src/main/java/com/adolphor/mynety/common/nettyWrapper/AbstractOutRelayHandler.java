package com.adolphor.mynety.common.nettyWrapper;

import com.adolphor.mynety.common.utils.SocksServerUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.adolphor.mynety.common.constants.Constants.LOG_MSG_OUT;
import static com.adolphor.mynety.common.constants.Constants.REQUEST_TEMP_LIST;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * 带有缓存的远程连接处理器：
 * 1.覆写增加了LOG日志和channel关闭方法
 * 2.消费缓存的请求信息
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public abstract class AbstractOutRelayHandler<I> extends AbstractSimpleHandler<I> {

  protected final Channel clientChannel;
  protected final List requestTempList;

  public AbstractOutRelayHandler(Channel clientChannel) {
    this.clientChannel = clientChannel;
    this.requestTempList = clientChannel.attr(REQUEST_TEMP_LIST).get();
  }

  /**
   * 通道建立成功之后，立即消费缓存的数据
   *
   * @param ctx
   * @throws Exception
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
    if (requestTempList != null) {
      requestTempList.forEach(msg -> {
        ctx.channel().writeAndFlush(msg);
        logger.debug("[ {}{}{} ] [{}-channelActive] write temp msg({}) to des host: {}", clientChannel, LOG_MSG_OUT, ctx.channel(), getSimpleName(this), getSimpleName(msg), msg);
      });
      requestTempList.clear();
    } else {
      logger.info("[ {}{}{} ] [{}-channelActive] temp msg list is null...", clientChannel, LOG_MSG_OUT, ctx.channel(), getSimpleName(this));
    }
  }

  @Override
  protected void channelClose(ChannelHandlerContext ctx) {
    SocksServerUtils.flushOnClose(ctx.channel());
    SocksServerUtils.flushOnClose(clientChannel);
  }

}
