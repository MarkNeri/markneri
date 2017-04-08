package comp;

import comp.impl.DataRef;
import java.io.Serializable;
import java.util.function.Supplier;


public interface Ref<T extends Serializable> extends Supplier<T>, Serializable {

  public static <T extends Serializable, U extends T> Ref<T> ref(U data) {
    return DataRef.ref(data);
  }

  String digest();
}