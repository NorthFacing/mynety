package com.adolphor.mynety.client.adapter;

import com.adolphor.mynety.common.bean.Address;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import org.junit.Test;

import static com.adolphor.mynety.common.constants.Constants.ATTR_IN_RELAY_CHANNEL;
import static com.adolphor.mynety.common.constants.Constants.ATTR_REQUEST_ADDRESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SocksHandsShakeHandlerTest {

  @Test
  public void channelActive() {
    EmbeddedChannel channel = new EmbeddedChannel(SocksHandsShakeHandler.INSTANCE);
    ByteBuf readBuf = channel.readOutbound();
    assertEquals("050100", ByteBufUtil.hexDump(readBuf));
  }

  @Test
  public void channelRead0() {
    // 1. 如果协议类型不合法
    EmbeddedChannel channel = new EmbeddedChannel(SocksHandsShakeHandler.INSTANCE);
    ByteBuf buffer = Unpooled.buffer();
    buffer.writeBytes(new byte[]{0x05, 0x01});
    channel.pipeline().fireChannelRead(buffer);
    assertTrue(channel.finish());

    // 2. 协议类型合法
    ByteBuf readBuf;

    // 2.1 域名形式的IP地址
    channel = new EmbeddedChannel(SocksHandsShakeHandler.INSTANCE);
    channel.readOutbound();
    channel.attr(ATTR_IN_RELAY_CHANNEL).set(channel);
    buffer = Unpooled.buffer();
    buffer.writeBytes(new byte[]{0x05, 0x00});
    channel.attr(ATTR_REQUEST_ADDRESS).set(new Address("adolphor.com", 443));
    ReferenceCountUtil.retain(buffer);
    channel.pipeline().fireChannelRead(buffer);
    readBuf = channel.readOutbound();
    assertEquals("050100030c61646f6c70686f722e636f6d01bb", ByteBufUtil.hexDump(readBuf));

    // 2.2 IPv4 地址
    channel = new EmbeddedChannel(SocksHandsShakeHandler.INSTANCE);
    channel.readOutbound();
    channel.attr(ATTR_IN_RELAY_CHANNEL).set(channel);
    buffer = Unpooled.buffer();
    buffer.writeBytes(new byte[]{0x05, 0x00});
    channel.attr(ATTR_REQUEST_ADDRESS).set(new Address("185.199.111.153", 443));
    ReferenceCountUtil.retain(buffer);
    channel.pipeline().fireChannelRead(buffer);
    readBuf = channel.readOutbound();
    assertEquals("05010001b9c76f9901bb", ByteBufUtil.hexDump(readBuf));

    // 2.3 IPv6 地址

    ReferenceCountUtil.release(buffer);
  }
}