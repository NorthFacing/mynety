package com.adolphor.mynety.common.constants;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.6
 */
public class HandlerName {

  /**
   * common
   */
  public static final String loggingHandler = "loggingHandler";
  public static final String lanMessageDecoder = "lanMessageDecoder";
  public static final String lanMessageEncoder = "lanMessageEncoder";
  public static final String heartBeatHandler = "HeartBeatHandler";
  public static final String inBoundHandler = "inBoundHandler";
  public static final String outBoundHandler = "outBoundHandler";

  /**
   * client
   */
  public static final String socksPortUnificationServerHandler = "SocksPortUnificationServerHandler";
  public static final String socksAuthHandler = "socksAuthHandler";

  /**
   * server
   */
  public static final String addressHandler = "addressHandler";

  /**
   * lan
   */
  public static final String lanInBoundHandler = "lanInBoundHandler";
  public static final String lanOutBoundHandler = "lanOutBoundHandler";
  public static final String lanAdapterInBoundHandler = "lanAdapterInBoundHandler";

  /**
   * HTTP
   */
  public static final String sslClientHandler = "sslClientHandler";
  public static final String sslServerHandler = "sslServerHandler";
  public static final String httpClientCodec = "httpClientCodec";
  public static final String httpServerCodec = "httpServerCodec";
  public static final String httpAggregatorHandler = "httpAggregatorHandler";

  public static final String httpProxyHandler = "httpProxyHandler";
  public static final String socksShakerHandler = "socksShakerHandler";
  public static final String socksConnHandler = "socksConnHandler";

  public static final String httpInBoundHandler = "httpInBoundHandler";
  public static final String httpOutBoundHandler = "httpOutBoundHandler";



}
