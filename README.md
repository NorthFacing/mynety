
# Features

* Socks5 proxy
    - popular encryption methods supports
    - IP4, IP6 and domain supports
    - auto proxy by pac config：proxy，direct or deny
    - auto select the fastest one of available servers
* Http 1.1 proxy
    - http proxy over socks socks5
    - http tunnel proxy over socks5

## Package & run

Requires:
* Jdk8 or higher
* Maven

Shell commond:

```bash
# Packaging
mvn clean package -D maven.test.skip=true

# Running server
sh ./shadowsocks-java-server/target/ss-server/bin/mynety-server.sh
# Running client
sh ./shadowsocks-java-client/target/ss-client/bin/mynety-client.sh

# 后台启动
nohup sh mynety.sh >/dev/null 2>&1 &
```

# TODO list

* Traffic statistics
* DNS config (ignores localhost,127.0.0.1 and so on)
    - [用netty开发一个DNS server](https://zhuanlan.zhihu.com/p/39832709)
* Other protocols supported
* Http keep-alive supported
* redefine port number