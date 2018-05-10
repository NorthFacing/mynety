package com.adolphor.mynety.common.bean.lan;

import com.adolphor.mynety.common.utils.ByteStrUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import static com.adolphor.mynety.common.constants.LanConstants.ATTR_SERIAL_NO;

/**
 * lan客户端与代理服务器消息交换协议：
 * 4       +  1   +   8   +     4      +     L     +    4   +  M  +  N
 * 消息长度 + 类型 + 流水号 + 请求来源长度 + 请求来源ID + URI长度 + URI + 正式数据
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.5
 */
@Data
public class LanMessage {

  public static final int FRAME_SIZE = 4;
  public static final int HEADER_SIZE = 4 + 1 + 8 + 4 + 4;

  /**
   * 消息类型（1字节）
   */
  private byte type;

  /**
   * 流水号（long 数据类型占 8字节）
   */
  private long serialNumber = 0L;

  /**
   * 请求ID（绑定请求地址，LAN通道是单通道，通过此ID来明确请求的目标地址，使用32位UUID，16字节）
   */
  private String requestId;

  /**
   * 目标地址
   */
  private String uri;

  /**
   * 需要传输的数据
   */
  private byte[] data;


  public static Long getIncredSerNo(Channel channel) {
    synchronized (channel) {
      Long serNo = channel.attr(ATTR_SERIAL_NO).get();
      if (serNo == null) {
        serNo = 0L;
      }
      channel.attr(ATTR_SERIAL_NO).set(++serNo);
      return serNo;
    }
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer("LanMessage [")
        .append("type=").append(type)
        .append(", serNo=").append(serialNumber);
    if (StringUtils.isNotEmpty(requestId)) {
      sb.append(", requestId=").append(requestId);
    }
    if (StringUtils.isNotEmpty(uri)) {
      sb.append(", uri").append(uri);
    }
    sb.append("]");
    if (data != null && data.length > 0) {
      sb.append(" ==== LanMessage Data: ====\n");
      String str = ByteStrUtils.getString(Unpooled.wrappedBuffer(data));
      sb.append(str).append("\n");
    }
    return sb.toString();
  }

}
