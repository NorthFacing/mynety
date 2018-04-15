
# Features

* popular encryption methods supports
* IP4, IP6 and domain supports
* auto proxy by pac config
* auto change to the available server

## Package & run

```
mvn clean package
sh ./shadowsocks-java-server/target/ss-server/bin/ss-server.sh
sh ./shadowsocks-java-client/target/ss-client/bin/ss-client.sh
```

# TODO list

* looks like has a performance problem
* Traffic statistics
* HTTP protocal
* UDP protocal

# References

* [Netty - User guide for 4.x](http://netty.io/wiki/user-guide-for-4.x.html)
* [w3school - XSD Schema](http://www.w3school.com.cn/schema/index.asp)
* [Netty Socks代理服务器源码分析](https://alwayswithme.github.io/jekyll/update/2015/07/25/netty-socksproxy-detail.html)
* [wiki - SOCKS](https://en.wikipedia.org/wiki/SOCKS)
* [Netty - Native transports](https://github.com/netty/netty/wiki/Native-transports)



