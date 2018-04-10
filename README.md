
# Features

* popular encryption methods supports
* IP4, IP6 and domain supports
* auto proxy by pac config

## Package & run

* package then run

    ```
    mvn versions:set -DnewVersion=0.0.1-SNAPSHOT
    mvn clean package
    sh ./shadowsocks-java-server/target/ss-server/bin/ss-server.sh
    sh ./shadowsocks-java-client/target/ss-client/bin/ss-client.sh
    ```

* Run in the background

    - unix & linux

    ```Shell
    nohup sh ss-client.sh >/dev/null 2>&1 &
    nohup sh ss-server.sh >/dev/null 2>&1 &
    ```
    - windows
    add blew codes at the head of the bat file:

    ```Bat
    @echo off 
    if "%1" == "h" goto begin 
    mshta vbscript:createobject("wscript.shell").run("%~nx0 h",0)(window.close)&&exit 
    :begin 
    ```

# References

* [Netty - User guide for 4.x](http://netty.io/wiki/user-guide-for-4.x.html)
* [w3school - XSD Schema](http://www.w3school.com.cn/schema/index.asp)
* [Netty Socks代理服务器源码分析](https://alwayswithme.github.io/jekyll/update/2015/07/25/netty-socksproxy-detail.html)
* [wiki - SOCKS](https://en.wikipedia.org/wiki/SOCKS)


