package common;

import java.util.concurrent.ExecutionException;

public class Exceptions {

  public static RuntimeException toRuntime(Throwable e) {
    if (e instanceof ExecutionException) {
      return toRuntime(((ExecutionException) e).getCause());
    } else if (e instanceof RuntimeException) {
      return (RuntimeException) e;
    } else {
      return new RuntimeException(e);
    }
  }
}
