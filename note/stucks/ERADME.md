# CPU占用过高的问题

打开client客户端，浏览器访问几个页面之后，就会出现CPU占用过高的问题。

```
$ top
$ ps aux | grep 48451
$ jstack 48451 > stack.txt
```

经过排查发现大部分线程卡在PacFilter中，主要的就是正则过滤执行时间过长。
后来发现是正则表达式写的有问题，具体正则规则后续分析：

* 有问题的正则
    - Pattern.matches("^(\\w?.?)+" + conf, domain)
* 改造后的正则
    - Pattern.matches("([a-z0-9]+[.])*" + conf, domain)


# HTTP代理

## 编解码

http 的 tunnel 代理直接连接，去掉所有编解码器之后就可以进行盲转了，
但是 HTTP 代理，在进行转发的时候需要加上编解码器，这样 proxy 和 server 
进行数据交换的时候，才能够识别到正确的 HTTP 内容。

## HttpObjectAggregator
当 HTTP content 内容太大的时候，是不是会出现内容不全的情况？

好像会，改成分段模式吧：

* HttpRequest（1个）
* HttpContent（可能有 0~N 个 content）
* LastHttpContent（1个）

## 信息发送之后没反应
首先考虑编解码问题，怎么可以快速查看当前channel的所有handler？


# Netty 相关问题

## io.netty.util.IllegalReferenceCountException: refCnt: 0, increment: 1

这个问题的原因是释放msg的时候发现msg的引用计数为0，解决方法是在传递msg的地方使用retain()方法
告诉netty此消息传递给channel中的下个handler进行消费。


## Netty中ctx.writeAndFlush与ctx.channel().writeAndFlush的区别
https://blog.csdn.net/FishSeeker/article/details/78447684

先说实验结论就是 ctx 的writeAndFlush是从当前handler直接发出这个消息，
而 channel 的writeAndFlush是从整个pipline最后一个outhandler发出。

## netty channelActive 方法
只有channel建立的时候会会调用，当增加handler或者删除handler的时候，除非手动调用，否则不再执行active方法。

Q: 如果有多个handler，那么每个handler中的active方法都会执行吗？



