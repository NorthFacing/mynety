
## 打包 & 运行

* 打包和运行

    ```
    # 版本管理
    mvn versions:set -DnewVersion=0.0.1-SNAPSHOT
    # 编译和打包
    mvn clean package
    # 运行
    sh ./shadowsocks-java-server/target/ss-server/bin/ss-server.sh
    sh ./shadowsocks-java-client/target/ss-client/bin/ss-client.sh
    ```

* 后台运行

    - unix & linux

    ```Shell
    nohup sh ss-client.sh >/dev/null 2>&1 &
    nohup sh ss-server.sh >/dev/null 2>&1 &
    ```
    - windows
    在代码头部加一段代码就可以了

    ```Bat
    @echo off 
    if "%1" == "h" goto begin 
    mshta vbscript:createobject("wscript.shell").run("%~nx0 h",0)(window.close)&&exit 
    :begin 
    ```

# 参考

* [Netty - User guide for 4.x](http://netty.io/wiki/user-guide-for-4.x.html)
* [w3school - XSD Schema](http://www.w3school.com.cn/schema/index.asp)
* [Netty Socks代理服务器源码分析](https://alwayswithme.github.io/jekyll/update/2015/07/25/netty-socksproxy-detail.html)
* [wiki - SOCKS](https://en.wikipedia.org/wiki/SOCKS)


