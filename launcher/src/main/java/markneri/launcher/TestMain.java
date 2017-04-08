package markneri.launcher;

import java.io.IOException;

/**
 * Created by markneri on 3/17/2017.
 */
public class TestMain {

  public static void main(String[] args) throws InterruptedException, IOException {
    new RemoteExecute().username("ubuntu").jvmWithArgs("jdk1.8.0_121/jre/bin/java").remoteExecute(args, "54.91.149.144");

    System.out.println(Runtime.getRuntime().availableProcessors());
    for (int i = 0; i < 100; i++) {
      System.out.println(i);
    }


  }
}
