# netty错误

不是在自己的代码中报错，怎么处理？

```
[2018-05-02 20:28:45 ERROR] 22293 [KQueueEventLoopGroup-4-2] com.shadowsocks.client.httpAdapter.tunnel.HttpTunnelRemoteHandler.exceptionCaught(72): RemoteHandler error
io.netty.util.IllegalReferenceCountException: refCnt: 0, increment: 1
	at io.netty.buffer.AbstractReferenceCountedByteBuf.release0(AbstractReferenceCountedByteBuf.java:100) ~[netty-all-4.1.24.Final.jar:4.1.24.Final]
	at io.netty.buffer.AbstractReferenceCountedByteBuf.release(AbstractReferenceCountedByteBuf.java:84) ~[netty-all-4.1.24.Final.jar:4.1.24.Final]
	at io.netty.util.ReferenceCountUtil.release(ReferenceCountUtil.java:88) ~[netty-all-4.1.24.Final.jar:4.1.24.Final]
	at io.netty.channel.SimpleChannelInboundHandler.channelRead(SimpleChannelInboundHandler.java:112) ~[netty-all-4.1.24.Final.jar:4.1.24.Final]
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362) [netty-all-4.1.24.Final.jar:4.1.24.Final]
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348) [netty-all-4.1.24.Final.jar:4.1.24.Final]
	at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340) [netty-all-4.1.24.Final.jar:4.1.24.Final]
	at io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1434) [netty-all-4.1.24.Final.jar:4.1.24.Final]
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362) [netty-all-4.1.24.Final.jar:4.1.24.Final]
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348) [netty-all-4.1.24.Final.jar:4.1.24.Final]
	at io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:965) [netty-all-4.1.24.Final.jar:4.1.24.Final]
	at io.netty.channel.kqueue.AbstractKQueueStreamChannel$KQueueStreamUnsafe.readReady(AbstractKQueueStreamChannel.java:551) [netty-all-4.1.24.Final.jar:4.1.24.Final]
	at io.netty.channel.kqueue.AbstractKQueueChannel$AbstractKQueueUnsafe.readReady(AbstractKQueueChannel.java:399) [netty-all-4.1.24.Final.jar:4.1.24.Final]
	at io.netty.channel.kqueue.KQueueEventLoop.processReady(KQueueEventLoop.java:195) [netty-all-4.1.24.Final.jar:4.1.24.Final]
	at io.netty.channel.kqueue.KQueueEventLoop.run(KQueueEventLoop.java:269) [netty-all-4.1.24.Final.jar:4.1.24.Final]
	at io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:884) [netty-all-4.1.24.Final.jar:4.1.24.Final]
	at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30) [netty-all-4.1.24.Final.jar:4.1.24.Final]
	at java.lang.Thread.run(Thread.java:748) [?:1.8.0_162]
```