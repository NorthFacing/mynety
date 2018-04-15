# CPU占用过高的问题

打开client客户端，浏览器访问几个页面之后，就会出现CPU占用过高的问题。

```
$ top
$ ps aux | grep 48451
$ jstack 48451 > stack.txt
```





