package com.adolphor.mynety.common.bean.lan;

import com.adolphor.mynety.common.constants.LanMsgType;
import com.adolphor.mynety.common.utils.BaseUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class LanMessageEncoderTest {

  /**
   * Msg type => {@link LanMsgType}
   */
  @Test
  public void encode() {
    M00_Heart_Beat();
    M01_Client();
    M02_Connect();
    M03_Connected();
    M04_Transmit();
  }

  /**
   * not null params: requestId,type,serial number
   */
  private void M00_Heart_Beat() {
    LanMessage message = new LanMessage();
    LanMsgType type = LanMsgType.HEARTBEAT;
    Long sequenceNum = Long.valueOf(BaseUtils.getRandomInt(1000, 9999));
    message.setType(type);
    message.setSequenceNum(sequenceNum);

    EmbeddedChannel encodeChannel = new EmbeddedChannel(new LanMessageEncoder());
    Assert.assertTrue(encodeChannel.writeOutbound(message));
    Assert.assertTrue(encodeChannel.finish());
    ByteBuf encodeMsg = encodeChannel.readOutbound();

    ReferenceCountUtil.retain(encodeMsg);

    EmbeddedChannel decodeChannel = new EmbeddedChannel(new LanMessageDecoder());
    Assert.assertTrue(decodeChannel.writeInbound(encodeMsg));
    Assert.assertTrue(decodeChannel.finish());
    LanMessage decodeMsg = decodeChannel.readInbound();

    Assert.assertEquals(type, decodeMsg.getType());
    Assert.assertEquals(sequenceNum, decodeMsg.getSequenceNum());
  }

  private void M01_Client(){

  }

  /**
   * not null params: requestId,type,uri
   */
  private void M02_Connect() {
    LanMessage message = new LanMessage();
    String uuid = BaseUtils.getUUID();
    LanMsgType type = LanMsgType.CONNECT;
    String uri = "adolphor.com:443";

    message.setType(type);
    message.setRequestId(uuid);
    message.setUri(uri);

    EmbeddedChannel encodeChannel = new EmbeddedChannel(new LanMessageEncoder());
    Assert.assertTrue(encodeChannel.writeOutbound(message)); // write inbound msg
    Assert.assertTrue(encodeChannel.finish()); // change channel state to finish

    ByteBuf encodeMsg = encodeChannel.readOutbound(); // get the result msg that cross the pipeline => ByteBuf

    EmbeddedChannel decodeChannel = new EmbeddedChannel(new LanMessageDecoder());
    Assert.assertTrue(decodeChannel.writeInbound(encodeMsg));
    Assert.assertTrue(decodeChannel.finish());

    LanMessage decodeMsg = decodeChannel.readInbound();

    Assert.assertEquals(type, decodeMsg.getType());
    Assert.assertEquals(uuid, decodeMsg.getRequestId());
    Assert.assertEquals(uri, decodeMsg.getUri());
  }

  /**
   * not null params: requestId,type
   */
  private void M03_Connected() {
    LanMessage message = new LanMessage();
    String uuid = BaseUtils.getUUID();
    LanMsgType type = LanMsgType.CONNECTED;

    message.setType(type);
    message.setRequestId(uuid);

    EmbeddedChannel encodeChannel = new EmbeddedChannel(new LanMessageEncoder());
    Assert.assertTrue(encodeChannel.writeOutbound(message));
    Assert.assertTrue(encodeChannel.finish());
    ByteBuf encodeMsg = encodeChannel.readOutbound();

    EmbeddedChannel decodeChannel = new EmbeddedChannel(new LanMessageDecoder());
    Assert.assertTrue(decodeChannel.writeInbound(encodeMsg));
    Assert.assertTrue(decodeChannel.finish());
    LanMessage decodeMsg = decodeChannel.readInbound();

    Assert.assertEquals(type, decodeMsg.getType());
    Assert.assertEquals(uuid, decodeMsg.getRequestId());
  }

  /**
   * not null params: requestId,type,data
   */
  private void M04_Transmit() {
    LanMessage message = new LanMessage();
    LanMsgType type = LanMsgType.TRANSMIT;
    String data = "hello encode & decode ~";
    message.setType(type);
    message.setData(data.getBytes(StandardCharsets.UTF_8));

    EmbeddedChannel encodeChannel = new EmbeddedChannel(new LanMessageEncoder());
    Assert.assertTrue(encodeChannel.writeOutbound(message));
    Assert.assertTrue(encodeChannel.finish());
    ByteBuf encodeMsg = encodeChannel.readOutbound();

    EmbeddedChannel decodeChannel = new EmbeddedChannel(new LanMessageDecoder());
    Assert.assertTrue(decodeChannel.writeInbound(encodeMsg));
    Assert.assertTrue(decodeChannel.finish());
    LanMessage decodeMsg = decodeChannel.readInbound();

    Assert.assertEquals(type, decodeMsg.getType());
    Assert.assertEquals(data, new String(decodeMsg.getData(), StandardCharsets.UTF_8));
  }

}