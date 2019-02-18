package com.adolphor.mynety.common.constants;

import io.netty.util.AttributeKey;

/**
 * LAN 相关常量
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
public class LanConstants {

  public static final int READ_IDLE_TIME = 45;
  public static final int WRITE_IDLE_TIME = 30;
  public static final int ALL_IDLE_TIME = 60;

  /**
   * 容忍的最大缺失心跳数
   */
  public static final int MAX_IDLE_TIMES_LIMIT = 3;

  /**
   * 断线重连等待时间递增，此为等待初始值
   */
  public static final int INIT_SLEEP_TIME = 1000;
  /**
   * 断线重连等待时间递增，此为等待最大值
   */
  public static final int MAX_SLEEP_TIME = 6000;


  // 协议相关: https://iluoxuan.iteye.com/blog/1849302
  /**
   * 信息最大长度，超过这个长度会报异常
   */
  public static final int MAX_FRAME_LENGTH = 2 * 1024 * 1024;
  /**
   * 长度属性的起始（偏移）位，我们的协议中长度是0到第4个字节，所以这里写0
   */
  public static final int LENGTH_FIELD_OFFSET = 0;
  /**
   * “长度属性”的长度，我们是4个字节，所以写4
   */
  public static final int LENGTH_FIELD_LENGTH = 4;
  /**
   * 长度调节值，在总长被定义为包含包头长度时，修正信息长度
   */
  public static final int LENGTH_ADJUSTMENT = -LENGTH_FIELD_LENGTH;
  /**
   * 跳过的字节数，以便接收端直接接受到不含“长度属性”的内容
   */
  public static final int INITIAL_BYTES_TO_STRIP = LENGTH_FIELD_LENGTH;

  /**
   *
   */
  public static final AttributeKey<Boolean> IS_MAIN_CHANNEL = AttributeKey.valueOf("is.main.channel");
  public static final AttributeKey<Long> ATTR_SERIAL_NO = AttributeKey.valueOf("serial.number");
  public static final AttributeKey<Long> ATTR_LAST_BEAT_NO = AttributeKey.valueOf("last.beat.number");
  public static final AttributeKey<Long> ATTR_LOST_BEAT_CNT = AttributeKey.valueOf("lost.beat.number");

}
