
# 主要功能

* 内网穿透
    - 最优美的内网穿透解决方案
* Socks5 代理
    - 支持IPv4和域名访问，IPv6暂不支持
    - 根据配置选择代理方式：代理、直连和拒绝连接
    - 自动选择优质线路
* HTTP 代理
    - 支持 HTTP 和 HTTPS tunnel

## 配置要求

* Jdk8 或以上
* Maven

## 启动和运行

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

# TODO

* Traffic statistics
* DNS config (ignores localhost,127.0.0.1 and so on)
    - [用netty开发一个DNS server](https://zhuanlan.zhihu.com/p/39832709)
* Other protocols supported
* Http keep-alive supported