package com.adolphor.mynety.client;

import com.adolphor.mynety.client.config.Config;
import com.adolphor.mynety.client.config.ConfigLoader;
import com.adolphor.mynety.client.http.HttpInBoundInitializer;
import com.adolphor.mynety.client.utils.NetUtils;
import com.adolphor.mynety.client.utils.cert.CertUtils;
import com.adolphor.mynety.common.constants.Constants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static com.adolphor.mynety.client.config.Config.HTTPS_CERT_CONFIG;
import static com.adolphor.mynety.common.constants.Constants.LOG_LEVEL;

/**
 * entrance of client
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
public final class ClientMain {

  public static void main(String[] args) throws Exception {

    ConfigLoader.loadConfig();
    NetUtils.checkServers();

    new Thread(() -> {
      EventLoopGroup sBossGroup = null;
      EventLoopGroup sWorkerGroup = null;
      try {
        sBossGroup = (EventLoopGroup) Constants.bossGroupType.newInstance();
        sWorkerGroup = (EventLoopGroup) Constants.bossGroupType.newInstance();
        ServerBootstrap sServerBoot = new ServerBootstrap();
        sServerBoot.group(sBossGroup, sWorkerGroup)
            .channel(Constants.serverChannelClass)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(InBoundInitializer.INSTANCE);
        String sLocalHost = Config.IS_PUBLIC ? Constants.ALL_LOCAL_ADDRESS : Constants.LOOPBACK_ADDRESS;
        ChannelFuture sFuture = sServerBoot.bind(sLocalHost, Config.SOCKS_PROXY_PORT).sync();
        sFuture.channel().closeFuture().sync();
      } catch (Exception e) {
        logger.error("", e);
      } finally {
        sBossGroup.shutdownGracefully();
        sWorkerGroup.shutdownGracefully();
      }
    }, "socks-proxy-thread").start();

    new Thread(() -> {
      EventLoopGroup hBossGroup = null;
      EventLoopGroup hWorkerGroup = null;
      try {
        if (Config.HTTP_MITM) {
          X509Certificate caCert = CertUtils.loadCert(Config.CA_KEYSTORE_FILE, Config.CA_PASSWORD.toCharArray());
          PrivateKey caPriKey = CertUtils.loadPriKey(Config.CA_KEYSTORE_FILE, Config.CA_PASSWORD.toCharArray());
          SslContext sslCtx = SslContextBuilder.forClient()
              .trustManager(InsecureTrustManagerFactory.INSTANCE)
//              .applicationProtocolConfig(new ApplicationProtocolConfig(
//                  ApplicationProtocolConfig.Protocol.ALPN,
//                  ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
//                  ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
//                  ApplicationProtocolNames.HTTP_2,
//                  ApplicationProtocolNames.HTTP_1_1))
              .build();
          HTTPS_CERT_CONFIG.setClientSslCtx(sslCtx);
          HTTPS_CERT_CONFIG.setIssuer(caCert.getIssuerDN().toString());
          HTTPS_CERT_CONFIG.setNotBefore(caCert.getNotBefore());
          HTTPS_CERT_CONFIG.setNotAfter(caCert.getNotAfter());
          HTTPS_CERT_CONFIG.setCaPriKey(caPriKey);
          KeyPair keyPair = CertUtils.genKeyPair();
          HTTPS_CERT_CONFIG.setMitmPriKey(keyPair.getPrivate());
          HTTPS_CERT_CONFIG.setMitmPubKey(keyPair.getPublic());
        }
        hBossGroup = (EventLoopGroup) Constants.bossGroupType.newInstance();
        hWorkerGroup = (EventLoopGroup) Constants.bossGroupType.newInstance();
        ServerBootstrap hServerBoot = new ServerBootstrap();
        hServerBoot.group(hBossGroup, hWorkerGroup)
            .channel(Constants.serverChannelClass)
            .handler(new LoggingHandler(LOG_LEVEL))
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(HttpInBoundInitializer.INSTANCE);
        String hLocalHost = Config.IS_PUBLIC ? Constants.ALL_LOCAL_ADDRESS : Constants.LOOPBACK_ADDRESS;
        ChannelFuture hFuture = hServerBoot.bind(hLocalHost, Config.HTTP_PROXY_PORT).sync();
        hFuture.channel().closeFuture().sync();
      } catch (Exception e) {
        logger.error("", e);
      } finally {
        hBossGroup.shutdownGracefully();
        hWorkerGroup.shutdownGracefully();
      }
    }, "http/https-proxy-thread").start();

  }
}
