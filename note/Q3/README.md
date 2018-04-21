# socks5 TO http/https

### 当前将socks5转为http代理的组件
* Polipo
* privoxy

### socks5拦截HTTP请求，解析出现乱码
socks5 代理浏览器或者当做系统代理的时候，在client加密使用 new String(arr,"UTF-8")会出现乱码，
为什么会出现乱码？浏览器插件或者系统代理对HTTP请求做了什么处理？

