package com.adolphor.mynety.client.config;

import com.adolphor.mynety.common.encryption.CryptFactory;
import com.adolphor.mynety.common.encryption.ICrypt;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.List;

/**
 * 加载配置信息
 * <p>
 * load config info
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.1
 */
@Slf4j
public class ConfigLoader {

  private static String configFileName = "client-config.xml";

  /**
   * 分别加载 pac 和 client 和 server 配置信息
   * <p>
   * load pac & client & server conf info
   *
   * @throws Exception
   */
  public static void loadConfig() throws Exception {
    // PAC 模式配置
    String pacFile = "pac.xml";
    loadPac(pacFile);

    // 服务器资源配置
    // 先加载测试环境配置，如果为空再去找正式环境配置（为了调试开发方便）
    loadClientConf("dev-" + configFileName);
    if (ClientConfig.getAvailableServer() == null) {
      loadClientConf(configFileName);
    }
  }

  private static void loadPac(String pacFile) throws Exception {
    DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
    DocumentBuilder domBuilder = domfac.newDocumentBuilder();
    try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(pacFile)) {
      if (is == null) {
        return;
      }
      Document doc = domBuilder.parse(is);
      Element root = doc.getDocumentElement();
      addDomains(root, "proxy", ProxyPacConfig.PROXY_DOMAINS);
      addDomains(root, "direct", ProxyPacConfig.DIRECT_DOMAINS);
      addDomains(root, "deny", ProxyPacConfig.DENY_DOMAINS);
    }
  }


  private static void addDomains(Element root, String strategy, List<String> domainList) {

    NodeList proxys = root.getElementsByTagName(strategy);

    if (proxys == null) {
      return;
    }

    for (int i = 0; i < proxys.getLength(); i++) {
      Node proxy = proxys.item(i);
      if (proxy.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      NodeList domains = proxy.getChildNodes();
      if (domains == null) {
        return;
      }

      for (int j = 0; j < domains.getLength(); j++) {
        Node domain = domains.item(j);
        if (domain.getNodeType() != Node.ELEMENT_NODE) {
          continue;
        }

        String name = domain.getNodeName();
        switch (name) {
          case "domain":
            String value = domain.getFirstChild().getNodeValue();
            domainList.add(value);
            break;
          default:
            break;
        }
      }
    }
    logger.debug("读取{}代理配置完毕：{}个", strategy, domainList.size());
  }


  private static void loadClientConf(String configFile) throws Exception {

    DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
    DocumentBuilder domBuilder = domfac.newDocumentBuilder();

    try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(configFile)) {
      if (is == null) {
        // dev文件为空直接返回接着找pro文件；如果pro文件也为空，就说明缺少配置文件
        if (configFileName.equals(configFile)) {
          throw new NullPointerException("缺少 client-config.xml 配置文件！");
        }
        return;
      }
      Document doc = domBuilder.parse(is);
      Element root = doc.getDocumentElement();
      NodeList configs = root.getChildNodes();
      if (configs == null) {
        return;
      }

      for (int i = 0; i < configs.getLength(); i++) {
        Node node = configs.item(i);
        if (node.getNodeType() != Node.ELEMENT_NODE) {
          continue;
        }

        String nodeName = node.getNodeName();
        String value = node.getFirstChild().getNodeValue();
        switch (nodeName) {
          case "public":
            ClientConfig.IS_PUBLIC = Boolean.valueOf(value);
            break;
          case "socksLocalPort":
            try {
              ClientConfig.SOCKS_PROXY_PORT = Integer.valueOf(value);
            } catch (Exception e) {
              logger.error("本地端口配置 socksLocalPort 参数不合法，将使用默认端口1086！");
            }
            break;
          case "httpLocalPort":
            try {
              ClientConfig.HTTP_PROXY_PORT = Integer.valueOf(value);
            } catch (Exception e) {
              logger.error("本地端口配置 httpLocalPort 参数不合法，将使用默认端口1087！");
            }
            break;
          case "proxyStrategy":
            try {
              ClientConfig.PROXY_STRATEGY = Integer.valueOf(value);
            } catch (Exception e) {
              logger.error("代理模式配置 proxyStrategy 参数不合法，将使用全局代理模式！");
            }
            break;
          case "http2socks5":
            try {
              ClientConfig.HTTP_2_SOCKS5 = Boolean.valueOf(value);
            } catch (Exception e) {
              logger.error("代理模式配置 http2socks5 参数不合法，将使用socks5二次代理http请求！");
            }
            break;
          case "servers":
            getServers(node);
            break;
          default:
            logger.warn("Unknown config for proxy client: {}={}", nodeName, value);
            break;
        }
      }
    }
  }

  private static void getServers(Node serversWrapperNode) {
    if (serversWrapperNode.getNodeType() != Node.ELEMENT_NODE
        || !"servers".equals(serversWrapperNode.getNodeName())) {
      return;
    }

    NodeList serverWrapperChildNodes = serversWrapperNode.getChildNodes();
    for (int j = 0; j < serverWrapperChildNodes.getLength(); j++) {
      Node serverWrapperNode = serverWrapperChildNodes.item(j);

      if (serversWrapperNode.getNodeType() != Node.ELEMENT_NODE
          || !"server".equals(serverWrapperNode.getNodeName())) {
        continue;
      }

      NodeList serverChildNodes = serverWrapperNode.getChildNodes();

      Server bean = new Server();
      ClientConfig.addServer(bean);

      for (int k = 0; k < serverChildNodes.getLength(); k++) {
        Node serverChild = serverChildNodes.item(k);

        if (serverChild.getNodeType() != Node.ELEMENT_NODE) {
          continue;
        }

        String name = serverChild.getNodeName();
        String val = serverChild.getFirstChild().getNodeValue();
        switch (name) {
          case "remarks":
            bean.setRemarks(val);
            break;
          case "host":
            bean.setHost(val);
            break;
          case "port":
            bean.setPort(Integer.valueOf(val));
            break;
          case "method":
            bean.setMethod(val);
            break;
          case "password":
            bean.setPassword(val);
            break;
          default:
            logger.warn("Unknown config for proxy client of server node: {}={}", name, val);
            break;
        }
      }
      ConfigLoader.getEncrypt(bean);
      logger.debug("Proxy server config loaded：{}", bean);
    }
    logger.debug("Proxy server config loads success!");
  }

  /**
   * 根据用户名和密码获取加密参数
   *
   * @param server 服务器信息
   */
  private static void getEncrypt(Server server) {
    String method = server.getMethod();
    String password = server.getPassword();
    ICrypt crypt = CryptFactory.get(method, password);
    server.setCrypt(crypt);
  }

}
