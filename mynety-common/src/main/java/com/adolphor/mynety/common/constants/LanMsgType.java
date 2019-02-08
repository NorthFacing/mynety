package com.adolphor.mynety.common.constants;

/**
 * LAN 相关常量
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.6
 */
public enum LanMsgType {

  HEARTBEAT("心跳消息", (byte) 0x00),
  AUTH("权限验证", (byte) 0x01),
  CONNECT("建立连接", (byte) 0x02),
  DISCONNECT("断开连接", (byte) 0x03),
  TRANSFER("数据转发", (byte) 0x04);

  private String remark;
  private byte val;

  LanMsgType(String remark, byte val) {
    this.remark = remark;
    this.val = val;
  }

  public byte getVal() {
    return this.val;
  }

  public static LanMsgType getType(byte val) {
    LanMsgType[] values = LanMsgType.values();
    for (LanMsgType type : values) {
      if (type.val == val) {
        return type;
      }
    }
    return null;
  }

}
