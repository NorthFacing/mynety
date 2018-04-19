
# Features

* Socks5 protocol
* popular encryption methods supports
* IP4, IP6 and domain supports
* auto proxy by pac config：proxy，direct or deny
* auto select the fastest one of available servers

## Package & run

Requres:
* Jdk8 or higher
* Maven

Shell commond:

```
mvn clean package
sh ./shadowsocks-java-server/target/ss-server/bin/ss-server.sh
sh ./shadowsocks-java-client/target/ss-client/bin/ss-client.sh
```

# TODO list

* Traffic statistics
* HTTP protocal
* UDP protocal
* DNS config (such as localhost,127.0.0.1 and so on)


# References

* [Netty - User guide for 4.x](http://netty.io/wiki/user-guide-for-4.x.html)
* [w3school - XSD Schema](http://www.w3school.com.cn/schema/index.asp)
* [Netty Socks代理服务器源码分析](https://alwayswithme.github.io/jekyll/update/2015/07/25/netty-socksproxy-detail.html)
* [wiki - SOCKS](https://en.wikipedia.org/wiki/SOCKS)
* [Netty - Native transports](https://github.com/netty/netty/wiki/Native-transports)
* [Netty之有效规避内存泄漏](http://m635674608.iteye.com/blog/2236834)
* [netty5 HTTP协议栈浅析与实践](http://www.cnblogs.com/cyfonly/p/5616493.html)
* [基于Netty4构建HTTP服务----浏览器访问和Netty客户端访问](https://blog.csdn.net/wangshuang1631/article/details/73251180)
* [How to use Socks 5 proxy with Apache HTTP Client 4?](https://stackoverflow.com/questions/22937983/how-to-use-socks-5-proxy-with-apache-http-client-4)




