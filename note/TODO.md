
## HTTP代理使用如下迭代方式实现：
* HTTP 常规代理转发：需要解析HTTP头等信息之后进行转发
* tunnel 隧道代理：建立隧道之后进行盲转发
    - 普通代理是否可以转为隧道代理进行转发？普通代理无需转为隧道代理进行转发（隧道代理的服务端会进行特殊的处理而不是HTTP响应）
* ssl/tls 是否可以直接通过隧道代理进行转发？当然可以
* FTP，telnet可以通过隧道代理进行转发，那么服务端是怎么处理的？像处理隧道中的ssl请求一样进行处理
* 以上代理完毕之后，加上socks5代理
    - 直接加上一层壳？
    - 将HTTP转为socks5？采用了将HTTP转为socks5

### 建立隧道连接的过程
1.client使用connect方法请求proxy
2.proxy解析client请求的目标地址
3.proxy建立绑定目标地址socket，建立连接不需要协议认证过程，且此时不发送任何数据
4.proxy响应client，返回200
5.移除解码器
6.转发双方数据

### 隧道嵌套socks5

```
telnet 127.0.0.1 1187

CONNECT msfwifi.3g.qq.com:8080 HTTP/1.1
Host: msfwifi.3g.qq.com:8080
Proxy-Authorization: Basic Og==
Proxy-Connection: Keep-Alive
```

### HTTP 连接通信过程
1.client请求proxy，使用的编解码器：serverCodec
2.proxy解析client请求的目标地址
3.proxy建立绑定目标地址socket，使用的编解码器：clientCodec（一定要使用编解码，才可以在channel中直接write HttpRequest或者HttpResponse）
4.并发送本次建立连接的request请求到目标服务器
5.之后的数据经过编解码相互转发

```bash
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

# github program

* worked
    - alanzplus/HTTP-Proxy


* deleted
    - geosolutions-it/http-proxy
    