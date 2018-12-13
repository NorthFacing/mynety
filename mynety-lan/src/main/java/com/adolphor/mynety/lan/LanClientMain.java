package com.adolphor.mynety.lan;

import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.lan.config.Config;
import com.adolphor.mynety.lan.config.ConfigLoader;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
public class LanClientMain {

  private static Bootstrap bootstrap;
  private static EventLoopGroup workerGroup;

  public static void main(String[] args) throws Exception {

    ConfigLoader.loadConfig();

    try {

      doConnect();

    } catch (Exception e) {
      logger.error("lan客户端启动出错：：", e);
    } finally {
      if (workerGroup != null) {
        workerGroup.shutdownGracefully();
      }
    }

  }

  /**
   * 抽取出该方法 (断线重连时使用)
   *
   * @throws InterruptedException
   */
  public static void doConnect() {
    reconnectWait();
    try {
      bootstrap = new Bootstrap();
      workerGroup = (EventLoopGroup) Constants.bossGroupClass.getDeclaredConstructor().newInstance();
      bootstrap.group(workerGroup)
          .channel(Constants.channelClass)
          .option(ChannelOption.TCP_NODELAY, true)
          .handler(new LanPipelineInitializer());

      ChannelFuture future = bootstrap.connect(Config.LAN_SERVER_HOST, Config.LAN_SERVER_PORT).sync();
      future.channel().closeFuture().sync();
    } catch (Exception e) {
      doConnect();
    }
  }

  private static long sleepTimeMill = 1000;

  public static void reconnectWait() {
    try {
      if (sleepTimeMill > 60000) {
        sleepTimeMill = 1000;
      }
      synchronized (LanClientMain.class) {
        sleepTimeMill = sleepTimeMill * 2;
        Thread.sleep(sleepTimeMill);
      }
    } catch (InterruptedException e) {
      logger.error("reconnectWait error:", e);
    }
  }


}
