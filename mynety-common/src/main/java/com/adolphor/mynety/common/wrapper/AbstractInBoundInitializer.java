package com.adolphor.mynety.common.wrapper;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

import static com.adolphor.mynety.common.constants.Constants.ATTR_OUT_RELAY_CHANNEL_REF;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_TEMP_MSG;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * 建立连接的时候初始化相关默认属性值
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
@ChannelHandler.Sharable
public abstract class AbstractInBoundInitializer extends ChannelInitializer<SocketChannel> {

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    logger.debug("[ {} ]【{}】调用 initChannel 方法开始……", ch, getSimpleName(this));
    final AtomicReference<Channel> outRelayChannelRef = new AtomicReference<>();
    ch.attr(ATTR_OUT_RELAY_CHANNEL_REF).set(outRelayChannelRef);
    logger.debug("[ {} ]【{}】设置 outRelayChannelRef 属性：{}", ch.id(), getSimpleName(this), outRelayChannelRef);
    AtomicReference tempMsgRef = new AtomicReference();
    ch.attr(ATTR_REQUEST_TEMP_MSG).set(tempMsgRef);
    logger.debug("[ {} ]【{}】设置 requestTempMsg 属性：{}", ch.id(), getSimpleName(this), tempMsgRef.getClass());
  }

}
