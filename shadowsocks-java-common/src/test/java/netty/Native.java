package netty;

import java.util.Map;

public class Native {

  public static void main(String[] args) {
    String property = System.getProperty("os.name");
    System.out.println(property);
  }


  private static String getComputerName() {
    Map<String, String> env = System.getenv();
    if (env.containsKey("COMPUTERNAME"))
      return env.get("COMPUTERNAME");
    else if (env.containsKey("HOSTNAME"))
      return env.get("HOSTNAME");
    else
      return "Unknown Computer";
  }
}
