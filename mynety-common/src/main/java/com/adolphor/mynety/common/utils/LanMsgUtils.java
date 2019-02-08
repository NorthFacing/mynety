package com.adolphor.mynety.common.utils;

import com.adolphor.mynety.common.bean.lan.LanMessage;
import com.adolphor.mynety.common.constants.LanMsgType;
import io.netty.channel.Channel;

/**
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
public class LanMsgUtils {

  /**
   * 拼装lan通信时的消息对象
   *
   * @param relayChannel 通信channel
   * @param requestId    请求ID
   * @param msgType      消息类型
   * @return 组装好的lan消息对象
   */
  public static LanMessage packageLanMsg(Channel relayChannel, String requestId, byte msgType) {
    LanMessage lanConnMsg = new LanMessage();
    lanConnMsg.setType(LanMsgType.getType(msgType));
    lanConnMsg.setSerialNumber(LanMessage.getIncredSerNo(relayChannel));
    lanConnMsg.setRequestId(requestId);
    return lanConnMsg;
  }


}
