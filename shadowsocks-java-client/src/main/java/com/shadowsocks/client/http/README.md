# Http 代理

实现http代理，并增加一层socks5代理的壳，，之后所有消息的流转完整顺序如下：

* 不需要 socks5 server 时：
```
client <--> socks5 client <--> dst server
```
* 需要 socks5 server 时：
```
client <--> socks5 client <--> socks5 server <--> dst server
```