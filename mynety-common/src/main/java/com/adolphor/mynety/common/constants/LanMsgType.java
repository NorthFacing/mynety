package com.adolphor.mynety.common.constants;

/**
 * optional operation type of lan msg
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.6
 */
public enum LanMsgType {

  HEARTBEAT((byte) 0x00),
  AUTH((byte) 0x01),
  CONNECT((byte) 0x02),
  DISCONNECT((byte) 0x03),
  TRANSFER((byte) 0x04);

  private byte val;

  LanMsgType(byte val) {
    this.val = val;
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

  public byte getVal() {
    return this.val;
  }

}
