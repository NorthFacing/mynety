/**
 * MIT License
 * <p>
 * Copyright (c) 2018 0haizhu0@gmail.com
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.shadowsocks.common.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;

/**
 * 本地缓存工具
 *
 * @author 0haizhu0@gmail.com
 * @since v0.0.3
 */
@Slf4j
public class LocalCache {

  private static final Map<String, ValueObject> localCache = new ConcurrentHashMap<>();

  public LocalCache() {
  }

  /**
   * 增加缓存
   *
   * @param key      键
   * @param newValue 值
   * @param timeout  有效时间（毫秒）
   * @return 原值
   */
  public static String set(String key, String newValue, long timeout) {
    long currentTime = System.currentTimeMillis();
    ValueObject valueObject = localCache.get(key);
    long newTimeOut = timeout == 0 ? 0 : (currentTime + timeout); // 新的超时时间
    if (localCache.keySet().contains(key) && (valueObject.getTimeout() == 0 || currentTime <= valueObject.getTimeout())) {
      String oldValue = valueObject.getValue();
      valueObject.timeout = newTimeOut;
      valueObject.value = newValue;
      return oldValue; // 更新的话返回原来的缓存值
    } else {
      localCache.put(key, new ValueObject(newValue, newTimeOut));
      return null; // 新增/或已经超时的话无返回
    }
  }

  /**
   * 增加缓存（永久有效）
   *
   * @param key   键
   * @param value 值
   * @return 原值
   */
  public static String set(String key, String value) {
    return set(key, value, 0);
  }

  /**
   * @param key 键
   * @return key对应的value
   */
  public static String get(String key) {
    long currentTime = System.currentTimeMillis();
    ValueObject valueObject = localCache.get(key);
    if (valueObject == null) {
      return null;
    }
    if (valueObject.getTimeout() == 0 || currentTime <= valueObject.getTimeout()) {
      return valueObject.getValue();
    } else {
      localCache.remove(key);
      return null;
    }
  }

  /**
   * @return 缓存数据量
   */
  public static int size() {
    return localCache.size();
  }

  /**
   * 清空数据
   *
   * @return
   */
  public static boolean clear() {
    localCache.clear();
    return true;
  }

  /**
   * @param sizeLimit 超过limit值才进行处理，提高效率
   */
  public static void validateForGC(int sizeLimit) {
    if (size() < sizeLimit)
      return;
    logger.debug("{} before resize keys: {}", LOG_MSG, size());
    localCache.forEach((k, v) -> {
      long currentTime = System.currentTimeMillis();
      if (v.getTimeout() != 0 && currentTime >= v.getTimeout()) {
        localCache.remove(k);
        logger.debug("{} remove key: {}", LOG_MSG, k);
      }
    });
    logger.debug("{} after resize keys: {}", LOG_MSG, size());
  }

  /**
   * value Object
   */
  @Data
  private static class ValueObject {

    private String value;
    private long timeout;

    private ValueObject(String value, long timeout) {
      super();
      this.value = value;
      this.timeout = timeout;
    }
  }

}