# 内网穿透

本文件夹内代码主要是支持实现内网穿透。

## 内网映射

如果是将运行客户端的机器映射到外网，比如对外开放80，直接使用如下的方式进行访问即可：
`mynetylan.adolphor.com:80`

如果是将局域网内的其它机器映射到外网，以后再说吧~

## 内网转发

* 配置为转发所有请求到内网服务器
* 配置需要转发的地址，形如：`<host>:<port>`
