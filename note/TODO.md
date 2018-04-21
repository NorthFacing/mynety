
## HTTP代理使用如下迭代方式实现：
* HTTP 常规代理转发：需要解析HTTP头等信息之后进行转发
* tunnel 隧道代理：建立隧道之后进行盲转发
    - 普通代理是否可以转为隧道代理进行转发？普通代理无需转为隧道代理进行转发（隧道代理的服务端会进行特殊的处理而不是HTTP响应）
* ssl/tls 是否可以直接通过隧道代理进行转发？当然可以
* FTP，telnet可以通过隧道代理进行转发，那么服务端是怎么处理的？像处理隧道中的ssl请求一样进行处理
* 以上代理完毕之后，加上socks5代理
    - 直接加上一层壳？
    - 将HTTP转为socks5？

### 建立隧道连接的过程
1.client使用connect方法请求proxy
2.proxy解析client请求的目标地址
3.proxy建立绑定目标地址socket，建立连接不需要协议认证过程，且此时不发送任何数据
4.proxy响应client，返回200
5.移除解码器
6.转发双方数据

### HTTP 连接通信过程
1.client请求proxy
2.proxy解析client请求的目标地址
3.proxy建立绑定目标地址socket，并发送本次request请求到目标服务器
4.转发双方数据

# github program

* worked
    - alanzplus/HTTP-Proxy


* deleted
    - geosolutions-it/http-proxy
    