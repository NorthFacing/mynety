# NoSuchElementException

在pipeline中移除handler的时候出现NoSuchElementException，但之前并没有移除？


# Netty中ctx.writeAndFlush与ctx.channel().writeAndFlush的区别
https://blog.csdn.net/FishSeeker/article/details/78447684

先说实验结论就是 ctx 的writeAndFlush是从当前handler直接发出这个消息，
而 channel 的writeAndFlush是从整个pipline最后一个outhandler发出。


# netty channelActive 方法
只有channel建立的时候会会调用，当增加handler或者删除handler的时候，除非手动调用，否则不再执行active方法。

Q: 如果有多个handler，那么每个handler中的active方法都会执行吗？

