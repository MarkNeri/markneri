package comp;

import comp.impl.GeneratorRef;
import java.io.Serializable;
import java.util.function.Supplier;


@FunctionalInterface
public interface Generator<T extends Serializable> extends Supplier<T>, Serializable {

  static <T extends Serializable, U extends T> Ref<T> schedule(Generator<U> generator) {
    return GeneratorRef.compute(generator);
  }

}
