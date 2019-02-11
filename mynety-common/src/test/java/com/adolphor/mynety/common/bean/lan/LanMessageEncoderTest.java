package com.adolphor.mynety.common.bean.lan;

import com.adolphor.mynety.common.constants.LanMsgType;
import com.adolphor.mynety.common.utils.BaseUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class LanMessageEncoderTest {

  /**
   * Msg type => {@link com.adolphor.mynety.common.constants.LanMsgType}
   */
  @Test
  public void encode() {
    M00_Heart_Beat();
    M02_Connect();
    M03_Disconnect();
    M04_Transfer_Data();
  }

  /**
   * not null params: requestId,type,serial number
   */
  private void M00_Heart_Beat() {
    LanMessage message = new LanMessage();
    String uuid = BaseUtils.getUUID();
    LanMsgType type = LanMsgType.HEARTBEAT;
    Long serialNumber = Long.valueOf(BaseUtils.getRandomInt(1000, 9999));
    message.setRequestId(uuid);
    message.setType(type);
    message.setSequenceNumber(serialNumber);

    EmbeddedChannel encodeChannel = new EmbeddedChannel(new LanMessageEncoder());
    Assert.assertTrue(encodeChannel.writeOutbound(message));
    Assert.assertTrue(encodeChannel.finish());
    ByteBuf encodeMsg = encodeChannel.readOutbound();

    EmbeddedChannel decodeChannel = new EmbeddedChannel(new LanMessageDecoder());
    Assert.assertTrue(decodeChannel.writeInbound(encodeMsg));
    Assert.assertTrue(decodeChannel.finish());
    LanMessage decodeMsg = decodeChannel.readInbound();

    Assert.assertEquals(uuid, decodeMsg.getRequestId());
    Assert.assertEquals(type, decodeMsg.getType());
    Assert.assertEquals(serialNumber, decodeMsg.getSequenceNumber());
  }

  /**
   * not null params: requestId,type,uri
   */
  private void M02_Connect() {
    LanMessage message = new LanMessage();
    String uuid = BaseUtils.getUUID();
    LanMsgType type = LanMsgType.CONNECT;
    String uri = "adolphor.com:443";
    message.setRequestId(uuid);
    message.setType(type);
    message.setUri(uri);

    EmbeddedChannel encodeChannel = new EmbeddedChannel(new LanMessageEncoder());
    Assert.assertTrue(encodeChannel.writeOutbound(message)); // write inbound msg
    Assert.assertTrue(encodeChannel.finish()); // change channel state to finish

    ByteBuf encodeMsg = encodeChannel.readOutbound(); // get the result msg that cross the pipeline => ByteBuf

    EmbeddedChannel decodeChannel = new EmbeddedChannel(new LanMessageDecoder());
    Assert.assertTrue(decodeChannel.writeInbound(encodeMsg));
    Assert.assertTrue(decodeChannel.finish());

    LanMessage decodeMsg = decodeChannel.readInbound();

    Assert.assertEquals(uuid, decodeMsg.getRequestId());
    Assert.assertEquals(type, decodeMsg.getType());
    Assert.assertEquals(uri, decodeMsg.getUri());
  }

  /**
   * not null params: requestId,type
   */
  private void M03_Disconnect() {
    LanMessage message = new LanMessage();
    String uuid = BaseUtils.getUUID();
    LanMsgType type = LanMsgType.DISCONNECT;

    message.setRequestId(uuid);
    message.setType(type);


    EmbeddedChannel encodeChannel = new EmbeddedChannel(new LanMessageEncoder());
    Assert.assertTrue(encodeChannel.writeOutbound(message));
    Assert.assertTrue(encodeChannel.finish());
    ByteBuf encodeMsg = encodeChannel.readOutbound();

    EmbeddedChannel decodeChannel = new EmbeddedChannel(new LanMessageDecoder());
    Assert.assertTrue(decodeChannel.writeInbound(encodeMsg));
    Assert.assertTrue(decodeChannel.finish());
    LanMessage decodeMsg = decodeChannel.readInbound();

    Assert.assertEquals(uuid, decodeMsg.getRequestId());
    Assert.assertEquals(type, decodeMsg.getType());
  }

  /**
   * not null params: requestId,type,data
   */
  private void M04_Transfer_Data() {
    LanMessage message = new LanMessage();
    String uuid = BaseUtils.getUUID();
    LanMsgType type = LanMsgType.TRANSFER;
    String data = "hello encoder & decoder !";
    message.setRequestId(uuid);
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

    Assert.assertEquals(uuid, decodeMsg.getRequestId());
    Assert.assertEquals(type, decodeMsg.getType());
    Assert.assertEquals(data, new String(decodeMsg.getData(), StandardCharsets.UTF_8));
  }

}