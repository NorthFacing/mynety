package com.shadowsocks.server.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

/**
 * 加载xml格式config
 */
public class ConfigLoader {

	private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

	public static void loadServer() throws Exception {

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
						Config.LOCAL_PORT = Integer.valueOf(value);
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
		logger.debug("配置加载完毕：Port={}, method={}, password={}",
			Config.LOCAL_PORT, Config.METHOD, Config.PASSWORD);
	}

}
