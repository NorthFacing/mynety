package com.adolphor.mynety.common.encryption;

import java.lang.reflect.Constructor;

/**
 * reflect costs: 16611 ms
 * cache costs:   8209 ms
 * direct new:    8216 ms
 */
public class ReflectTest {

  public static void main(String[] args) throws Exception {

    long start1 = System.currentTimeMillis();
    for (int i = 0; i < 1000000; i++) {
      Class<?> clazz = Class.forName(TestDomain.class.getName());
      Constructor<?> constructor = clazz.getConstructor();
      TestDomain instance = (TestDomain) constructor.newInstance();
      instance.print(i);
    }
    long end1 = System.currentTimeMillis();

    long start4 = System.currentTimeMillis();
    Constructor<?> constructor4 = null;
    for (int i = 0; i < 1000000; i++) {
      Class<?> clazz = Class.forName(TestDomain.class.getName());
      constructor4 = clazz.getConstructor();
    }
    TestDomain instance4 = (TestDomain) constructor4.newInstance();
    instance4.print(4);
    long end4 = System.currentTimeMillis();

    Class<?> clazz = Class.forName(TestDomain.class.getName());
    Constructor<?> constructor = clazz.getConstructor();
    long start2 = System.currentTimeMillis();
    for (int i = 0; i < 1000000; i++) {
      TestDomain instance = (TestDomain) constructor.newInstance();
      instance.print(i);
    }
    long end2 = System.currentTimeMillis();

    long start3 = System.currentTimeMillis();
    for (int i = 0; i < 1000000; i++) {
      TestDomain instance = new TestDomain();
      instance.print(i);
    }
    long end3 = System.currentTimeMillis();

    System.out.println("reflect costs: " + (end1 - start1));
    System.out.println("sub ref costs: " + (end4 - start4));
    System.out.println("cache costs:   " + (end2 - start2));
    System.out.println("direct new:    " + (end3 - start3));
  }

}

