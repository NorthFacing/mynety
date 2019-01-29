package com.adolphor.mynety.client.socks;

import com.adolphor.mynety.common.constants.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import static com.adolphor.mynety.client.socks.SocksClientMainTest.DST_HOST;

@Slf4j
public class Socks03ConnectHandler extends SimpleChannelInboundHandler {

  private final ByteBuf buf;

  /**
   * 发送的消息格式：
   * +----+-----+-------+------+----------+----------+
   * |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.ATTR_PORT |
   * +----+-----+-------+------+----------+----------+
   * | 1  |  1  | X'00' |  1   | Variable |    2     |
   * +----+-----+-------+------+----------+----------+
   */
  public Socks03ConnectHandler() {
    buf = Unpooled.buffer();
    buf.writeByte(0x05);
    buf.writeByte(0x01);
    buf.writeByte(0x00);
    buf.writeByte(0x03); // domain
    byte[] bytes = DST_HOST.getBytes();
    buf.writeByte(bytes.length); // ADDR.LEN
    buf.writeBytes(bytes); // ADDR.LEN
    buf.writeShort(443); // port
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception{
    logger.info(Constants.LOG_MSG_OUT + ctx.channel() + "【连接】处理器激活，发送连接请求：" + ByteBufUtil.hexDump(buf));
    super.channelActive(ctx);
    ctx.writeAndFlush(buf);
  }

  /**
   * +----+-----+-------+------+----------+----------+
   * |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.ATTR_PORT |
   * +----+-----+-------+------+----------+----------+
   * | 1  |  1  | X'00' |  1   | Variable |    2     |
   * +----+-----+-------+------+----------+----------+
   *
   * @param ctx
   * @param msg
   * @throws Exception
   */
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    ByteBuf result = (ByteBuf) msg;
    byte ver = result.readByte();
    byte cmd = result.readByte();
    byte psv = result.readByte();
    byte atyp = result.readByte();

    byte dstLen = result.readByte();
    ByteBuf addrBuf = result.readBytes(dstLen);
    String addr = ByteBufUtil.hexDump(addrBuf);
    short port = result.readShort();
    logger.info(Constants.LOG_MSG_OUT + ctx.channel() + "【连接】处理器收到响应消息：ver={}, cmd={}, psv={}, atyp={}, dstLen={}, addr={}, port={}",
        ver, cmd, psv, atyp, dstLen, addr, port);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.pipeline().remove(this);
    ctx.pipeline().addLast(new Socks04DataHandler());
    logger.info(Constants.LOG_MSG_OUT + ctx.channel() + "【连接】处理器任务完成，添加【数据】处理器");
    ctx.pipeline().fireChannelActive();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
    logger.error(Constants.LOG_MSG_OUT + ctx.channel() + "【连接】处理器异常：", throwable);
    ctx.channel().close();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    logger.info(Constants.LOG_MSG_OUT + ctx.channel() + "【连接】处理器连接断开：" + ctx.channel());
    super.channelInactive(ctx);
  }

}
