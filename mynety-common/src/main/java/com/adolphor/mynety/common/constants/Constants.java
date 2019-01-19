package com.adolphor.mynety.common.constants;

import com.adolphor.mynety.common.bean.Address;
import com.adolphor.mynety.common.encryption.ICrypt;
import com.adolphor.mynety.common.wrapper.AbstractOutBoundHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * 常量
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
public class Constants {

  /**
   * 默认常用相关IP
   */
  public static final int PORT_80 = 80;
  public static final int PORT_443 = 443;
  public static final String SCHEME_HTTPS = "https";
  public static final String LOOPBACK_ADDRESS = "127.0.0.1";
  public static final String ALL_LOCAL_ADDRESS = "0.0.0.0";
  /**
   * 代理策略关键字
   */
  public static final String CONN_PROXY = "proxy";
  public static final String CONN_DIRECT = "direct";
  public static final String CONN_DENY = "deny";
  /**
   * 日志相关
   */
  public static final String LOG_MSG = " <==> ";
  public static final String LOG_MSG_OUT = " >>> ";
  public static final String LOG_MSG_IN = " <<< ";
  /**
   * 正则校验相关规则
   * backup:   ((http|ftp|https)://)(([a-zA-Z0-9._-]+)|([0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}))(([a-zA-Z]{2,6})|(:[0-9]{1,4})?)
   */
  public static final Pattern PATH_PATTERN = Pattern.compile("(?<=//|)((\\w)+\\.)+\\w+(:\\d{0,5})?");
  public static final Pattern IPV4_PATTERN = Pattern.compile("(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])");
  public static final Pattern IPV6_PATTERN = Pattern.compile("([0-9a-f]{1,4}:){3}([0-9a-f]){1,4}");

  /**
   * httpTunnel 代理返回值
   */
  public static final HttpResponseStatus CONNECTION_ESTABLISHED = new HttpResponseStatus(HttpResponseStatus.OK.code(), "Connection established");

  /**
   * 保留字节，填充空值
   */
  public static final int RESERVED_BYTE = 0x00;
  /**
   * 是否是http tunnel代理（inRelayChannel：httpProxy中使用）
   */
  public static final AttributeKey<Boolean> ATTR_IS_HTTP_TUNNEL = AttributeKey.valueOf("is.http.tunnel");
  /**
   * 是否是长连接（inRelayChannel）
   */
  public static final AttributeKey<Boolean> ATTR_IS_KEEP_ALIVE = AttributeKey.valueOf("keep.alive");
  /**
   * inRelayChannel (outRelayChannel)
   */
  public static final AttributeKey<Channel> ATTR_IN_RELAY_CHANNEL = AttributeKey.valueOf("in.relay.channel");
  /**
   * 是否使用代理（inRelayChannel，通道激活的时候就会赋值）
   */
  public static final AttributeKey<Boolean> ATTR_IS_PROXY = AttributeKey.valueOf("is.proxy");
  /**
   * outRelayChannel（inRelayChannel）
   */
  public static final AttributeKey<AtomicReference<Channel>> ATTR_OUT_RELAY_CHANNEL_REF = AttributeKey.valueOf("out.relay.channel.ref");
  /**
   * 详情参考 {@link AbstractOutBoundHandler#channelActive} 的备注
   */
  public static final AttributeKey<AtomicReference> ATTR_REQUEST_TEMP_MSG = AttributeKey.valueOf("request.temp.msg");
  /**
   * ss-local 建立连接的时候使用
   */
  public static final AttributeKey<Socks5CommandRequest> ATTR_SOCKS5_REQUEST = AttributeKey.valueOf("socks5.request");
  /**
   * 请求地址（inRelayChannel：client-httpProxy & server-AddressHandler 中使用）
   */
  public static final AttributeKey<Address> ATTR_REQUEST_ADDRESS = AttributeKey.valueOf("request.address");
  /**
   * 加密实例（inRelayChannel，因为IV不一样，所以每个通道建立连接的时候都需要初始化一个crypt实例）
   * 1. ss-local 对象初始化位于 {@link com.adolphor.mynety.client.InBoundHandler#channelActive(ChannelHandlerContext)}
   * 2. ss-server 对象初始化位于 {@link com.adolphor.mynety.server.AddressHandler#channelActive(ChannelHandlerContext)}}
   * 3. ss-lan 对象初始化位于 {@link com.adolphor.mynety.lan.LanInBoundHandler#channelActive(ChannelHandlerContext)}
   */
  public static final AttributeKey<ICrypt> ATTR_CRYPT_KEY = AttributeKey.valueOf("crypt");
  /**
   * 连接成功的时间戳（inRelayChannel，outRelayChannel）
   */
  public static final AttributeKey<Long> ATTR_CONNECTED_TIMESTAMP = AttributeKey.valueOf("is.connected");
  /**
   * 根据不同系统使用不同连接库
   */
  public static Class channelClass;
  public static Class serverChannelClass;
  public static Class bossGroupClass;
  public static Class workerGroupClass;

  // TODO 其实可以自定义继承官方channel自定义实现自己的channel的，这样更能了解channel的原理，知道channel各个步骤都干了啥
  static {
    if (SystemUtils.IS_OS_MAC) {
      logger.debug("macOS and BSD system ...");
      Constants.bossGroupClass = KQueueEventLoopGroup.class;
      Constants.workerGroupClass = KQueueEventLoopGroup.class;
      Constants.serverChannelClass = KQueueServerSocketChannel.class;
      Constants.channelClass = KQueueSocketChannel.class;
    } else if (SystemUtils.IS_OS_LINUX) {
      logger.debug("linux system...");
      Constants.bossGroupClass = EpollEventLoopGroup.class;
      Constants.workerGroupClass = EpollEventLoopGroup.class;
      Constants.serverChannelClass = EpollServerSocketChannel.class;
      Constants.channelClass = EpollSocketChannel.class;
    } else {
      logger.debug("others system...");
      Constants.bossGroupClass = NioEventLoopGroup.class;
      Constants.workerGroupClass = NioEventLoopGroup.class;
      Constants.serverChannelClass = NioServerSocketChannel.class;
      Constants.channelClass = NioSocketChannel.class;
    }

  }

}