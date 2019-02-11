package com.adolphor.mynety.common.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledHeapByteBuf;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.charset.StandardCharsets;

public class ByteStrUtilsTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void getDirectBuf() {
  }

  @Test
  public void getHeapBuf() {
  }

  @Test
  public void getStringByBuf() {
  }

  @Test
  public void getArrayByBuf() {
  }

  @Test
  public void writable() {
    String str = "Hello ByteBuf. ";
    String append = "Keep going.";

    byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
    byte[] appedBytes = append.getBytes(StandardCharsets.UTF_8);

    ByteBuf buf01 = Unpooled.buffer().writeBytes(strBytes);
    Assert.assertTrue(buf01 instanceof UnpooledHeapByteBuf);

    ByteBuf buf02 = buf01.writeBytes(appedBytes);
    Assert.assertEquals(str + append, ByteStrUtils.getStringByBuf(buf02));

    ByteBuf byteBuf = Unpooled.wrappedBuffer(strBytes);
    Assert.assertTrue(byteBuf instanceof UnpooledHeapByteBuf);

    thrown.expect(IndexOutOfBoundsException.class);
    thrown.expectMessage("writerIndex(15) + minWritableBytes(11) exceeds maxCapacity(15): UnpooledHeapByteBuf(ridx: 0, widx: 15, cap: 15/15)");

    byteBuf.writeBytes(appedBytes);

  }
}