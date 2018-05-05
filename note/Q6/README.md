# NoSuchElementException

在pipeline中移除handler的时候出现NoSuchElementException，但之前并没有移除？


# Netty中ctx.writeAndFlush与ctx.channel().writeAndFlush的区别
https://blog.csdn.net/FishSeeker/article/details/78447684

先说实验结论就是 ctx 的writeAndFlush是从当前handler直接发出这个消息，
而 channel 的writeAndFlush是从整个pipline最后一个outhandler发出。
