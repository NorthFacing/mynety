package com.shadowsocks.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;

public class LocalCache {

  private static final Logger logger = LoggerFactory.getLogger(LocalCache.class);

  private static final Map<String, ValueObject> localCache = new ConcurrentHashMap<>();

  public LocalCache() {
  }

  /**
   * @param key      键
   * @param newValue 值
   * @param timeout  超时时间（毫秒）
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
   * value Object
   */
  private static class ValueObject {

    private String value;
    private long timeout;

    private ValueObject(String value, long timeout) {
      super();
      this.value = value;
      this.timeout = timeout;
    }

    private String getValue() {
      return value;
    }

    private long getTimeout() {
      return timeout;
    }
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

}