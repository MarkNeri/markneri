package common;

import java.util.function.Supplier;

public interface ThreadLocals {

  static <T> Supplier<T> perThread(Supplier<T> supplier) {
    return new ThreadLocal<T>() {
      @Override
      protected T initialValue() {
        return supplier.get();
      }
    }::get;
  }
}
