package com.adolphor.mynety.lan;

import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.lan.config.Config;
import com.adolphor.mynety.lan.config.ConfigLoader;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.common.constants.LanConstants.INIT_SLEEP_TIME;
import static com.adolphor.mynety.common.constants.LanConstants.MAX_SLEEP_TIME;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
public class LanClientMain {

  private static Bootstrap bootstrap;
  private static EventLoopGroup workerGroup;
  private static long SLEEP_TIME = INIT_SLEEP_TIME;

  public static void main(String[] args) throws Exception {

    ConfigLoader.loadConfig();

    try {

      doConnect();

    } catch (Exception e) {
      logger.error("lan client start Error", e);
    } finally {
      if (workerGroup != null) {
        workerGroup.shutdownGracefully();
      }
    }

  }

  /**
   * 抽取出该方法 (断线重连时使用)
   *
   * @throws
   */
  public static void doConnect() {
    reconnectWait();
    try {
      bootstrap = new Bootstrap();
      workerGroup = (EventLoopGroup) Constants.bossGroupClass.getDeclaredConstructor().newInstance();
      bootstrap.group(workerGroup)
          .channel(Constants.channelClass)
          .handler(LanInBoundInitializer.INSTANCE);

      ChannelFuture future = bootstrap.connect(Config.LAN_SERVER_HOST, Config.LAN_SERVER_PORT).sync();
      future.channel().closeFuture().sync();

    } catch (Exception e) {
      logger.warn("", e);
      doConnect();
    }
  }

  public static void reconnectWait() {
    try {
      if (SLEEP_TIME > MAX_SLEEP_TIME) {
        SLEEP_TIME = INIT_SLEEP_TIME;
      }
      synchronized (LanClientMain.class) {
        SLEEP_TIME = SLEEP_TIME * 2;
        Thread.sleep(SLEEP_TIME);
      }
    } catch (InterruptedException e) {
      logger.error("Reconnect wait Error", e);
    }
  }


}
