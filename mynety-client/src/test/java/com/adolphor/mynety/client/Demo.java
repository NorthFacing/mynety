package com.adolphor.mynety.client;

public class Demo {

  Boolean isConn = true;

  public static void main(String[] args) throws Exception {

    Demo demo = new Demo();
//    new Thread(() -> demo.fun11()).start();
//    new Thread(() -> demo.fun22()).start();

    new Thread(() -> demo.fun1()).start();
    new Thread(() -> demo.fun2()).start();
  }

  private void fun11() {
    for (int i = 0; i < 10; i++) {
      slep("fun11");
      isConn = false;
      System.out.println("fun11: " + isConn);
    }
  }

  private void fun22() {
    for (int i = 0; i < 10; i++) {
      slep("fun22");
      isConn = true;
      System.out.println("fun22: " + isConn);
    }
  }

  private void slep(String name) {
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void fun1() {
    synchronized (isConn) {
      for (int i = 0; i < 10; i++) {
        slep("fun1");
        System.out.println("fun1: " + isConn);
      }
    }
  }

  private void fun2() {
    Boolean ano =isConn;
    synchronized (ano) {
      for (int i = 0; i < 10; i++) {
        slep("fun2");
        System.out.println("fun2: " + isConn);
      }
    }
  }


}
