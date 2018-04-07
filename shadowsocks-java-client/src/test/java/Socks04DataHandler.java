import com.shadowsocks.common.config.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Socks04DataHandler extends SimpleChannelInboundHandler {

	private static final Logger logger = LoggerFactory.getLogger(Socks03ConnectHandler.class);

	private final ByteBuf buf;

	public Socks04DataHandler() {
		buf = Unpooled.buffer();
		buf.writeByte(0x05);
		buf.writeByte(0x01);
		buf.writeByte(0x00);
		buf.writeByte(0x03); // domain
		byte[] bytes = "www.google.com".getBytes();
		buf.writeByte(bytes.length); // ADDR.LEN
		buf.writeBytes(bytes); // ADDR.LEN
		buf.writeShort(443); // port
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.info(Constants.LOG_MSG + ctx.channel() + "【数据】处理器激活，等待上次请求的返回结果...");
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		logger.info(Constants.LOG_MSG + ctx.channel() + "【数据】处理器收到消息：{}", msg);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		logger.info(Constants.LOG_MSG + ctx.channel() + "【数据】处理器数据处理完毕，发送新的网页请求" + ByteBufUtil.hexDump(buf));
		ctx.channel().writeAndFlush(buf);
//		logger.info(Constants.LOG_MSG + ctx.channel() + "【数据】处理器数据处理完毕");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
		logger.error(Constants.LOG_MSG + ctx.channel() + "【数据】处理器异常：", throwable);
		ctx.channel().close();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.info(Constants.LOG_MSG + ctx.channel() + "【数据】处理器连接断开：" + ctx.channel());
		super.channelInactive(ctx);
	}

}
