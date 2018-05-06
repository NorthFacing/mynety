/**
 * MIT License
 * <p>
 * Copyright (c) Bob.Zhu
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.shadowsocks.server.Config;

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
 * @Email 0haizhu0@gmail.com
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
          case "localPort":
            Config.PROXY_PORT = Integer.valueOf(value);
            break;
          case "method":
            Config.METHOD = value;
            break;
          case "password":
            Config.PASSWORD = value;
            break;
          default:
            break;

        }
      }
    }
    logger.debug("配置加载完毕：Port={}, method={}, password={}", Config.PROXY_PORT, Config.METHOD, Config.PASSWORD);
  }

}
