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
 *
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
					Node book = configs.item(i);
					if (book.getNodeType() == Node.ELEMENT_NODE) {
						String name = book.getNodeName();
						String value = book.getFirstChild().getNodeValue();
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

	public static void main(String[] args) throws Exception {
		Map<String, String> load = load("/Users/adolphor/IdeaProjects/bob/shadowsocks-java/shadowsocks-java-server/src/main/resources/Config.xml", Constants.config);
		System.out.println(load);
	}

}
