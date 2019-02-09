
# 主要功能

* 内网穿透
    - 最优美的内网穿透解决方案
* Socks5 代理
    - 支持IPv4和域名访问
    - 根据配置选择代理方式：代理、直连和拒绝连接
    - 自动选择优质线路
* HTTP 代理
    - 支持 HTTP 和 HTTP tunnel 
    - 支持 HTTPS 以及 MITM 
    - 支持 HTTP over Socks
* 兼容各平台ss客户端

## 配置要求

* Jdk8 或以上
* Maven

## 启动和运行

```bash
# Packaging
mvn clean package -D maven.test.skip=true

# Running server
sh ./mynety-server/target/mynety-server/bin/mynety-server.sh
# Running client
sh ./mynety-client/target/mynety-client/bin/mynety-client.sh
# Running lan
sh ./mynety-lan/target/mynety-lan/bin/mynety-lan.sh

# 后台启动
nohup sh mynety.sh >/dev/null 2>&1 &
```