
## Version 
```
mvn versions:set -DnewVersion=0.0.4-SNAPSHOT
```

## Debug

#### HTTP 连接通信过程
1.client请求proxy，使用的编解码器：serverCodec
2.proxy解析client请求的目标地址
3.proxy建立绑定目标地址socket，使用的编解码器：clientCodec（一定要使用编解码，才可以在channel中直接write HttpRequest或者HttpResponse）
4.并发送本次建立连接的request请求到目标服务器
5.之后的数据经过编解码相互转发

```
export http_proxy=http://0.0.0.0:1187;export https_proxy=http://0.0.0.0:1187;
curl http://adolphor.com/js/main.js?version=20180129

telnet 127.0.0.1 1187

GET http://adolphor.com/js/main.js?version=20180129 HTTP/1.1
Host: adolphor.com
Proxy-Connection: keep-alive
User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.139 Safari/537.36
Accept: */*
Referer: http://adolphor.com/
```

#### 建立隧道连接的过程
1.client使用connect方法请求proxy
2.proxy解析client请求的目标地址
3.proxy建立绑定目标地址socket，建立连接不需要协议认证过程，且此时不发送任何数据
4.proxy响应client，返回200
5.移除解码器（走http tunnel的未必是http请求，所以httpCodec不能使用）
6.转发双方数据

```
telnet 127.0.0.1 1187

CONNECT msfwifi.3g.qq.com:8080 HTTP/1.1
Host: msfwifi.3g.qq.com:8080
Proxy-Authorization: Basic Og==
Proxy-Connection: Keep-Alive

CONNECT 127.0.0.1:8080 HTTP/1.1
Host: 127.0.0.1:8080
Proxy-Authorization: Basic Og==
Proxy-Connection: Keep-Alive
```

## Run in the background

* unix & linux    

    ```Shell
    # config ss-client.sh:
    JAVA_HOME=../lib/jre8
    # Run:
    nohup sh ss-client.sh >/dev/null 2>&1 &
    nohup sh ss-server.sh >/dev/null 2>&1 &
    ```
* windows

    add blew codes at the head of the bat file:

    ```Bat
    @echo off 
    if "%1" == "h" goto begin 
    mshta vbscript:createobject("wscript.shell").run("%~nx0 h",0)(window.close)&&exit 
    :begin 
    
    :: 修改：JAVACMD
    if "%JAVACMD%"=="" set JAVACMD=..\lib\jre8\bin\java
    ```



