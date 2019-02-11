package com.adolphor.mynety.common.bean.lan;

import com.adolphor.mynety.common.constants.LanMsgType;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * DATA STRUCTURE:
 * <p>
 * +----------------+--------------+---------------+------------+----------------+
 * | message length | message type | serial number | request id | data content   |
 * | -------------- | ------------ | ------------- | ---------- | -------------- |
 * | 消息总长度       | 消息类型      |  流水序列号    | 请求来源ID  | 数据内容          |
 * +----------------+--------------+---------------+------------+----------------+
 * | 4 bytes        | 1 bytes      |  8 bytes      | 16 bytes   | dynamic (动态)  |
 * +----------------+--------------+---------------+------------+----------------+
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
@Data
public class LanMessage {


  public static final int HEADER_SIZE = 4 + 1 + 8 + 16;

  /**
   * message type (byte), refer: {@link com.adolphor.mynety.common.constants.LanMsgType}
   */
  private LanMsgType type;

  /**
   * sequence number of heart beat message
   */
  private Long sequenceNumber = 0L;

  /**
   * request id, to identify the request client/channel/domain:
   * 1. Be created before the msg be sent to the lan client.
   * 2. After received the result msg from lan, gets the request client by this id
   * 3. The id is compressed from 32 bytes to 16 bytes while encoded, to save network traffic
   */
  private String requestId;

  /**
   * destination address；
   */
  private String uri;

  /**
   * the message to be sent
   */
  private byte[] data;

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer("LanMessage [")
        .append("typeVal=").append(type.getVal())
        .append(", typeName=").append(type);
    sb.append(", serNo=").append(sequenceNumber);
    if (StringUtils.isNotEmpty(requestId)) {
      sb.append(", requestId=").append(requestId);
    }
    if (StringUtils.isNotEmpty(uri)) {
      sb.append(", uri=").append(uri);
    }
    if (data != null && data.length > 0) {
      sb.append(", data size=").append(data.length);
      sb.append(", data => ").append(data);
    }
    sb.append("]");
    return sb.toString();
  }

}
