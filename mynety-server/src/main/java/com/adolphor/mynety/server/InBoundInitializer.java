package com.adolphor.mynety.server;

import com.adolphor.mynety.common.wrapper.AbstractInBoundInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * 初始化处理器集
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
public class InBoundInitializer extends AbstractInBoundInitializer {

  public static final InBoundInitializer INSTANCE = new InBoundInitializer();

  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    super.initChannel(ch);
    logger.debug("[ {} ]【{}】调用 initChannel 方法开始……", ch, getSimpleName(this));
    ch.pipeline().addLast(AddressHandler.INSTANCE);
    logger.info("[ {} ]【PipelineInitializer】增加处理器: AddressHandler", ch.id());
  }
}
