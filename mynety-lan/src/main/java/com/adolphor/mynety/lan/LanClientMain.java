package com.adolphor.mynety.lan;

import com.adolphor.mynety.common.bean.lan.LanMessage;
import com.adolphor.mynety.common.constants.Constants;
import com.adolphor.mynety.common.utils.LanMsgUtils;
import com.adolphor.mynety.lan.config.Config;
import com.adolphor.mynety.lan.config.ConfigLoader;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;

import static com.adolphor.mynety.common.constants.LanConstants.INIT_SLEEP_TIME;
import static com.adolphor.mynety.common.constants.LanConstants.IS_MAIN_CHANNEL;
import static com.adolphor.mynety.common.constants.LanConstants.MAX_SLEEP_TIME;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Slf4j
public class LanClientMain {

  /**
   * after conn success, need to send a client msg to mark as a lan client main channel
   */
  private static final LanMessage lanClientMsg = LanMsgUtils.packClientMsg("password");
  private static long SLEEP_TIME = INIT_SLEEP_TIME;

  public static void main(String[] args) throws Exception {

    ConfigLoader.loadConfig();

    doConnect();

  }

  /**
   * to build mainChannel or requestChannel
   *
   * @return
   */
  public static void doConnect() throws Exception{
    reconnectWait();
    Bootstrap bootstrap = new Bootstrap();
    EventLoopGroup workerGroup = (EventLoopGroup) Constants.workerGroupType.newInstance();

    try {
      bootstrap.group(workerGroup)
          .channel(Constants.channelClass)
          .handler(InBoundInitializer.INSTANCE);

      ChannelFuture future = bootstrap.connect(Config.LAN_SERVER_HOST, Config.LAN_SERVER_PORT)
          .addListener((ChannelFutureListener) chFuture -> {
            if (chFuture.isSuccess()) {
              chFuture.channel().attr(IS_MAIN_CHANNEL).set(true);
              logger.info("lan main channel connect to lan server success...");
              chFuture.channel().writeAndFlush(lanClientMsg).addListener((ChannelFutureListener) innerFuture -> {
                if (innerFuture.isSuccess()) {
                  logger.info("send client main msg to lan server success ...");
                } else {
                  throw new ConnectException("connect failed: " + innerFuture.cause().getMessage());
                }
              });
            } else {
              throw new ConnectException("connect failed: " + chFuture.cause().getMessage());
            }
          })
          .sync();
      future.channel().closeFuture().sync();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      doConnect();
    } finally {
      if (workerGroup != null) {
        workerGroup.shutdownGracefully();
      }
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
