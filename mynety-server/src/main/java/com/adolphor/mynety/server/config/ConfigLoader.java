package com.adolphor.mynety.server.config;

import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

/**
 * 加载xml格式config
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.1
 */
@Slf4j
public class ConfigLoader {

  public static void loadConfig() throws Exception {

    String configFile = "server-config.xml";

    DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
    DocumentBuilder domBuilder = domfac.newDocumentBuilder();

    try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(configFile)) {
      Document doc = domBuilder.parse(is);
      Element root = doc.getDocumentElement();
      NodeList configs = root.getChildNodes();
      if (configs == null)
        return;

      for (int i = 0; i < configs.getLength(); i++) {
        Node server = configs.item(i);
        if (server.getNodeType() != Node.ELEMENT_NODE)
          continue;

        String name = server.getNodeName();
        String value = server.getFirstChild().getNodeValue();
        switch (name) {
          case "socksPort":
            Config.PROXY_PORT = Integer.valueOf(value);
            break;
          case "method":
            Config.METHOD = value;
            break;
          case "password":
            Config.PASSWORD = value;
            break;
          case "lanStrategy":
            Config.LAN_STRATEGY = Integer.valueOf(value);
            break;
          case "lanServerPort":
            Config.LAN_SERVER_PORT = Integer.valueOf(value);
            break;
          case "lanHostName":
            Config.LAN_HOST_NAME = value;
            break;
          default:
            logger.warn("Unknown config for proxy server: {}={}", name, value);
            break;
        }
      }
    }
    logger.debug("Proxy server config loaded：Port={}, method={}, password={}", Config.PROXY_PORT, Config.METHOD, Config.PASSWORD);
  }

}
