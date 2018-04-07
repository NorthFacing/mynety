import com.shadowsocks.client.RemoteHandler;
import com.shadowsocks.common.config.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 【握手】处理器：权限验证相关请求
 */
public class Socks01InitHandler extends SimpleChannelInboundHandler {

	private static final Logger logger = LoggerFactory.getLogger(RemoteHandler.class);

	private final ByteBuf buf;

	/**
	 * 发送的消息格式：
	 * +----+----------+----------+
	 * |VER | NMETHODS | METHODS  |
	 * +----+----------+----------+
	 * | 1  |    1     | 1 to 255 |
	 * +----+----------+----------+
	 */
	public Socks01InitHandler() {
		buf = Unpooled.buffer(3);
		buf.writeByte(0x05);
		buf.writeByte(0x01);
		buf.writeByte(0x00);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		logger.info(Constants.LOG_MSG + ctx.channel() + "【握手】处理器激活，发送初次访问请求：" + ByteBufUtil.hexDump(buf));
		ctx.channel().writeAndFlush(buf);
	}

	/**
	 * 接收到的消息格式：
	 * +----+----------+
	 * |VER |  METHOD  |
	 * +----+----------+
	 * | 1  |    1     |
	 * +----+----------+
	 *
	 * @param ctx
	 * @param msg
	 * @throws Exception
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf buf = (ByteBuf) msg;
		byte ver = buf.readByte();
		byte method = buf.readByte();
		logger.info(Constants.LOG_MSG + ctx.channel() + "【握手】处理器收到消息：ver={},method={}", ver, method);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.pipeline().remove(this);
		logger.info(Constants.LOG_MSG + ctx.channel() + "【握手】处理器任务完成，移除此处理器完毕");
		ctx.pipeline().addLast(new Socks03ConnectHandler());
		logger.info(Constants.LOG_MSG + ctx.channel() + "【握手】处理器任务完成，添加连接处理器");
		ctx.pipeline().fireChannelActive();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
		logger.error(Constants.LOG_MSG + ctx.channel() + "【握手】处理器异常：", throwable);
		ctx.channel().close();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.info(Constants.LOG_MSG + ctx.channel() + "【握手】处理器连接断开：" + ctx.channel());
		super.channelInactive(ctx);
	}

}
