package com.shadowsocks.common.utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LocalCacheTest {

  @Before
  public void before() {
    LocalCache.clear();
  }

  @Test
  public void cacheTest() throws InterruptedException {
    // set by time out
    String set1 = LocalCache.set("key1", "value11", 500);
    Assert.assertNull(set1); // 新增无返回值
    // 未超时的时候set会返回
    Thread.sleep(400);
    String set2 = LocalCache.set("key1", "value12", 500);
    Assert.assertEquals(set2, "value11"); // 返回原来的值
    Assert.assertEquals(LocalCache.get("key1"), "value12");
    // 超时的时候set无返回
    Thread.sleep(600);
    String set3 = LocalCache.set("key1", "value13", 500);
    Assert.assertNull(set3); // 已经超时，无返回值

    LocalCache.set("key2", "value2");
    // test size()
    Assert.assertEquals(LocalCache.size(), 2);
    Thread.sleep(500);
    String value1 = LocalCache.get("key1");
    Assert.assertNull(value1);
    String value2 = LocalCache.get("key2");
    Assert.assertNotNull(value2);
    // test size()：key1超时移除之后的缓存size
    Assert.assertEquals(LocalCache.size(), 1);
  }

  @Test
  public void reSizeTest() throws InterruptedException {
    for (int i = 0; i < 50; i++) {
      LocalCache.set("key1-" + i, "value1-" + i, 10 * 1000);
    }
    for (int i = 0; i < 50; i++) {
      LocalCache.set("key2-" + i, "value2-" + i, 30 * 1000);
    }
    Assert.assertEquals(LocalCache.size(), 100);
    LocalCache.validateForGC(10);
    Assert.assertEquals(LocalCache.size(), 100);
    Thread.sleep(10 * 1000);
    LocalCache.validateForGC(10);
    Assert.assertEquals(LocalCache.size(), 50);
  }

}