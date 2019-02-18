package com.adolphor.mynety.common.bean.lan;

import com.adolphor.mynety.common.constants.LanMsgType;
import com.adolphor.mynety.common.utils.BaseUtils;
import com.adolphor.mynety.common.utils.ByteStrUtils;
import io.netty.buffer.ByteBuf;
import lombok.Data;

import java.nio.charset.StandardCharsets;

/**
 * DATA STRUCTURE:
 * <p>
 * +----------------+--------------+----------------+
 * | message length | message type | data content   |
 * +----------------+--------------+----------------+
 * | 4 bytes        | 1 bytes      | dynamic (动态)  |
 * +----------------+--------------+----------------+
 * <p>
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Data
public class LanMessage {

  /**
   * message type (byte), refer: {@link LanMsgType}
   */
  private LanMsgType type;

  /**
   * the message to be sent
   */
  private byte[] data;

  private String password;
  private String requestId;
  private Long sequenceNum;
  private String uri;

  public byte[] getShortReqId() {
    return BaseUtils.compressUUID(requestId);
  }

  public void setRequestIdByBuf(ByteBuf idBuf) {
    this.requestId = BaseUtils.deCompressUUID(ByteStrUtils.readArrayByBuf(idBuf));
  }

  public void setSequenceNumByBuf(ByteBuf seqNumBuf) {
    byte[] sequenceNumArray = ByteStrUtils.readArrayByBuf(seqNumBuf);
    String sequenceNumStr = new String(sequenceNumArray, StandardCharsets.UTF_8);
    this.sequenceNum = Long.parseLong(sequenceNumStr);
  }

  public byte[] getBytesSequenceNum(){
    String strSequenceNum = String.valueOf(sequenceNum);
    byte[] sequenceNum = strSequenceNum.getBytes(StandardCharsets.UTF_8);
    return sequenceNum;
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer("LanMessage [")
        .append("typeVal=").append(type.getVal())
        .append(", typeName=").append(type);
    if (type == LanMsgType.CLIENT) {
      sb.append(", password=").append(password);
    } else if (type == LanMsgType.CONNECT) {
      sb.append(", requestId=").append(requestId);
      sb.append(", uri=").append(uri);
    } else if (type == LanMsgType.CONNECTED) {
      sb.append(", requestId=").append(requestId);
    } else if (type == LanMsgType.HEARTBEAT) {
      sb.append(", sequenceNum=").append(sequenceNum);
    } else if (type == LanMsgType.TRANSMIT) {
      sb.append(", data size=").append(data.length);
    }
    sb.append("]");
    return sb.toString();
  }

}
