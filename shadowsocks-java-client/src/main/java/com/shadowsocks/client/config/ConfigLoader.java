package com.shadowsocks.client.config;

import com.shadowsocks.common.encryption.CryptFactory;
import com.shadowsocks.common.encryption.ICrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.List;

/**
 * 加载xml格式config
 */
public class ConfigLoader {

	private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

	public static void loadConfig() throws Exception {
		loadPac();          // PAC 模式配置
		loadClientConf();   // 服务器资源
	}


	private static void loadPac() throws Exception {
		String configFile = "pac.xml";

		DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder domBuilder = domfac.newDocumentBuilder();

		try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(configFile)) {
			Document doc = domBuilder.parse(is);
			Element root = doc.getDocumentElement();

			addDomains(root, "proxy");
			addDomains(root, "direct");

		}

	}


	private static void addDomains(Element root, String strategy) {

		NodeList proxys = root.getElementsByTagName(strategy);
		List<String> domainList = "proxy".equalsIgnoreCase(strategy) ? PacConfig.proxyDomains : PacConfig.directDomains;

		if (proxys == null)
			return;

		for (int i = 0; i < proxys.getLength(); i++) {
			Node proxy = proxys.item(i);
			if (proxy.getNodeType() != Node.ELEMENT_NODE)
				continue;

			NodeList domains = proxy.getChildNodes();
			if (domains == null)
				return;

			for (int j = 0; j < domains.getLength(); j++) {
				Node domain = domains.item(j);
				if (domain.getNodeType() != Node.ELEMENT_NODE)
					continue;

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


	private static void loadClientConf() throws Exception {
		String configFile = "client-config.xml";

		DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder domBuilder = domfac.newDocumentBuilder();

		try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(configFile)) {
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
				String value = node.getFirstChild().getNodeValue();
				switch (nodeName) {
					case "public":
						ServerConfig.PUBLIC = Boolean.valueOf(value);
						break;
					case "localport":
						try {
							ServerConfig.LOCAL_PORT = Integer.valueOf(value);
						} catch (Exception e) {
							logger.error("本地端口配置 localport 参数不合法，将使用全局代理模式！");
						}
						break;
					case "proxyStrategy":
						try {
							ServerConfig.PROXY_STRATEGY = Integer.valueOf(value);
						} catch (Exception e) {
							logger.error("代理模式配置 proxyStrategy 参数不合法，将使用全局代理模式！");
						}
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
			ServerConfig.addServer(bean);

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
			ConfigLoader.getEncrypt(bean);
			logger.debug("加载服务器：{}", bean);
		}
		logger.debug("配置加载完毕!");
	}

	// 根据用户名和密码获取加密参数
	private static void getEncrypt(Server server) {
		String method = server.getMethod();
		String password = server.getPassword();
		ICrypt crypt = CryptFactory.get(method, password);
		server.setCrypt(crypt);
	}

}
