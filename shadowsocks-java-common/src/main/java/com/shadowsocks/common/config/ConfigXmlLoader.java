package com.shadowsocks.common.config;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * 加载xml格式config
 */
public class ConfigXmlLoader {

	public static Map<String, String> load(String file, Map<String, String> config) throws Exception {
		DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder domBuilder = domfac.newDocumentBuilder();
		try (InputStream is = new FileInputStream(new File(file))) {
			Document doc = domBuilder.parse(is);
			Element root = doc.getDocumentElement();
			NodeList configs = root.getChildNodes();
			if (configs != null) {
				for (int i = 0; i < configs.getLength(); i++) {
					Node server = configs.item(i);
					if (server.getNodeType() == Node.ELEMENT_NODE) {
						String name = server.getNodeName();
						String value = server.getFirstChild().getNodeValue();
						config.put(name, value);
					}
				}
				return config;
			}
		} catch (Exception e) {
			throw e;
		}
		return null;
	}

}
