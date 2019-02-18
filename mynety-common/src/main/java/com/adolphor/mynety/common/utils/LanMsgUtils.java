package com.adolphor.mynety.common.utils;

import com.adolphor.mynety.common.bean.lan.LanMessage;
import com.adolphor.mynety.common.constants.LanMsgType;
import io.netty.channel.Channel;

import static com.adolphor.mynety.common.constants.LanConstants.ATTR_SERIAL_NO;

/**
 * tools to package lan msg
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
public class LanMsgUtils {

  public static Long getNextNumber(Channel channel) {
    synchronized (channel) {
      Long seqNum = channel.attr(ATTR_SERIAL_NO).get();
      if (seqNum == null) {
        seqNum = 0L;
      }
      channel.attr(ATTR_SERIAL_NO).set(++seqNum);
      return seqNum;
    }
  }

  public static LanMessage packClientMsg(String password){
    LanMessage lanMessage = new LanMessage();
    lanMessage.setType(LanMsgType.CLIENT);
    lanMessage.setPassword(password);
    return lanMessage;
  }
  public static LanMessage packConnectMsg(String requestId,String uri){
    LanMessage lanMessage = new LanMessage();
    lanMessage.setRequestId(requestId);
    lanMessage.setUri(uri);
    lanMessage.setType(LanMsgType.CONNECT);
    return lanMessage;
  }
  public static LanMessage packConnectedMsg(String requestId){
    LanMessage lanMessage = new LanMessage();
    lanMessage.setType(LanMsgType.CONNECTED);
    lanMessage.setRequestId(requestId);
    return lanMessage;
  }
  public static LanMessage packTransferMsg(byte[] data){
    LanMessage lanMessage = new LanMessage();
    lanMessage.setType(LanMsgType.TRANSMIT);
    lanMessage.setData(data);
    return lanMessage;
  }
  public static LanMessage packHeartBeatMsg(Long sequenceNumber){
    LanMessage lanMessage = new LanMessage();
    lanMessage.setType(LanMsgType.HEARTBEAT);
    lanMessage.setSequenceNum(sequenceNumber);
    return lanMessage;
  }

}
