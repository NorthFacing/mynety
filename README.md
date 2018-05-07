
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
sh ./shadowsocks-java-server/target/ss-server/bin/ss-server.sh
# Running client
sh ./shadowsocks-java-client/target/ss-client/bin/ss-client.sh
```

# TODO list

* Traffic statistics
* DNS config (ignores localhost,127.0.0.1 and so on)
* Other protocols supported
* Http keep-alive supported