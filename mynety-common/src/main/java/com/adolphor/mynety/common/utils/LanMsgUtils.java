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

  public static LanMessage packageLanMsg(Channel relayChannel, String requestId, LanMsgType msgType) {
    LanMessage lanConnMsg = new LanMessage();
    lanConnMsg.setType(LanMsgType.getType(msgType.getVal()));
    lanConnMsg.setSequenceNumber(getNextNumber(relayChannel));
    lanConnMsg.setRequestId(requestId);
    return lanConnMsg;
  }

  public static Long getNextNumber(Channel channel) {
    Long serNo = channel.attr(ATTR_SERIAL_NO).get();
    if (serNo == null) {
      serNo = 0L;
    }
    channel.attr(ATTR_SERIAL_NO).set(++serNo);
    return serNo;
  }

}
