package com.adolphor.mynety.common.constants;


/**
 * LAN 代理策略枚举类
 *
 * @author Bob.Zhu
 * @Email adolphor@qq.com
 * @since v0.0.5
 */
public enum LanStrategy {

  /**
   * 关闭内网转发
   */
  CLOSE(-1),
  /**
   * 转发所有请求
   */
  ALL(1),
  /**
   * 转发自定义配置的请求
   */
  MANUAL(0);

  private int val;

  LanStrategy(int val) {
    this.val = val;
  }

  public static LanStrategy getLanStrategyByVal(int val) {
    for (LanStrategy lan : LanStrategy.values()) {
      if (lan.val == val) {
        return lan;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return this.name();
  }
}
