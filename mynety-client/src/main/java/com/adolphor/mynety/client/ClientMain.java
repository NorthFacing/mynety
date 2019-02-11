package com.adolphor.mynety.client;

import com.adolphor.mynety.client.config.ClientConfig;
import com.adolphor.mynety.client.config.ConfigLoader;
import com.adolphor.mynety.client.http.HttpInBoundInitializer;
import com.adolphor.mynety.client.utils.cert.CertUtils;
import com.adolphor.mynety.common.constants.Constants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static com.adolphor.mynety.client.config.ClientConfig.HTTPS_CERT_CONFIG;

/**
 * 客户端启动入口
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
public final class ClientMain {

  public static void main(String[] args) throws Exception {

    ConfigLoader.loadConfig();
    ClientConfig.checkServers();

    new Thread(() -> {
      EventLoopGroup sBossGroup = null;
      EventLoopGroup sWorkerGroup = null;
      try {
        sBossGroup = (EventLoopGroup) Constants.bossGroupClass.getDeclaredConstructor().newInstance();
        sWorkerGroup = (EventLoopGroup) Constants.bossGroupClass.getDeclaredConstructor().newInstance();
        ServerBootstrap sServerBoot = new ServerBootstrap();
        sServerBoot.group(sBossGroup, sWorkerGroup)
            .channel(Constants.serverChannelClass)
            .handler(new LoggingHandler(LogLevel.DEBUG))
            .childHandler(InBoundInitializer.INSTANCE);
        String sLocalHost = ClientConfig.IS_PUBLIC ? Constants.ALL_LOCAL_ADDRESS : Constants.LOOPBACK_ADDRESS;
        ChannelFuture sFuture = sServerBoot.bind(sLocalHost, ClientConfig.SOCKS_PROXY_PORT).sync();
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
        if (ClientConfig.HTTP_MITM) {
          X509Certificate caCert = CertUtils.loadCert(ClientConfig.CA_KEYSTORE_FILE, ClientConfig.CA_PASSWORD.toCharArray());
          PrivateKey caPriKey = CertUtils.loadPriKey(ClientConfig.CA_KEYSTORE_FILE, ClientConfig.CA_PASSWORD.toCharArray());
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
        hBossGroup = (EventLoopGroup) Constants.bossGroupClass.getDeclaredConstructor().newInstance();
        hWorkerGroup = (EventLoopGroup) Constants.bossGroupClass.getDeclaredConstructor().newInstance();
        ServerBootstrap hServerBoot = new ServerBootstrap();
        hServerBoot.group(hBossGroup, hWorkerGroup)
            .channel(Constants.serverChannelClass)
            .handler(new LoggingHandler(LogLevel.DEBUG))
            .childHandler(HttpInBoundInitializer.INSTANCE);
        String hLocalHost = ClientConfig.IS_PUBLIC ? Constants.ALL_LOCAL_ADDRESS : Constants.LOOPBACK_ADDRESS;
        ChannelFuture hFuture = hServerBoot.bind(hLocalHost, ClientConfig.HTTP_PROXY_PORT).sync();
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
