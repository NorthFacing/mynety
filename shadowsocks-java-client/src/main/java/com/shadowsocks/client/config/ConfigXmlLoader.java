package com.shadowsocks.client.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger logger = LoggerFactory.getLogger(ConfigXmlLoader.class);

	public static void loadClient(String fileName) throws Exception {
		// 兼容本地文件路径
		File file = new File(fileName);
		if (!file.exists()) {
			StringBuffer sb =
				new StringBuffer("shadowsocks-java-client").append(File.separator).append("src").append(File.separator)
					.append("main").append(File.separator).append("resources").append(File.separator).append(fileName);
			file = new File(sb.toString());
			if (!file.exists()) {
				logger.error("配置文件不存在：\n{}\n{}", fileName, sb.toString());
				throw new NullPointerException("找不到配置文件：" + fileName);
			}
		}

		DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder domBuilder = domfac.newDocumentBuilder();
		try (InputStream is = new FileInputStream(file)) {
			Document doc = domBuilder.parse(is);
			Element root = doc.getDocumentElement();
			NodeList configs = root.getChildNodes();
			if (configs == null)
				return;

			for (int i = 0; i < configs.getLength(); i++) {
				Node node = configs.item(i);
				if (node.getNodeType() != Node.ELEMENT_NODE)
					continue;

				String nodeName = node.getNodeName();
				switch (nodeName) {
					case "localport":
						String value = node.getFirstChild().getNodeValue();
						ClientConfig.LOCAL_PORT = Integer.valueOf(value);
						break;
					case "servers":
						getServers(node);
						break;
					default:
						break;
				}
			}
		}
	}

	private static void getServers(Node serversWrapperNode) {
		if (serversWrapperNode.getNodeType() != Node.ELEMENT_NODE
			|| !"servers".equals(serversWrapperNode.getNodeName()))
			return;

		NodeList serverWrapperChildNodes = serversWrapperNode.getChildNodes();
		for (int j = 0; j < serverWrapperChildNodes.getLength(); j++) {
			Node serverWrapperNode = serverWrapperChildNodes.item(j);

			if (serversWrapperNode.getNodeType() != Node.ELEMENT_NODE
				|| !"server".equals(serverWrapperNode.getNodeName()))
				continue;

			NodeList serverChildNodes = serverWrapperNode.getChildNodes();

			Server bean = new Server();
			ClientConfig.servers.add(bean);

			for (int k = 0; k < serverChildNodes.getLength(); k++) {
				Node serverChild = serverChildNodes.item(k);

				if (serverChild.getNodeType() != Node.ELEMENT_NODE)
					continue;

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
						break;
				}
			}
			logger.debug("加载服务器：{}", bean);
		}
		logger.debug("配置加载完毕：{}", ClientConfig.servers);
	}

}
