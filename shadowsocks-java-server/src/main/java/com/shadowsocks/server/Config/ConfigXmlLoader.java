package com.shadowsocks.server.Config;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * 加载xml格式config
 */
public class ConfigXmlLoader {

	public static void loadServer(String file) throws Exception {
		DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder domBuilder = domfac.newDocumentBuilder();
		try (InputStream is = new FileInputStream(new File(file))) {
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
						ServerConfig.config.setLocalPort(Integer.valueOf(value));
						break;
					case "method":
						ServerConfig.config.setMethod(value);
						break;
					case "password":
						ServerConfig.config.setPassword(value);
						break;
					default:
						break;

				}
			}
		}
	}

}
