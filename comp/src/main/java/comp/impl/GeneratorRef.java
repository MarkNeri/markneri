package comp.impl;

import com.google.common.collect.ImmutableList;
import common.Cache;
import common.Exceptions;
import comp.Generator;
import comp.Ref;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;


public class GeneratorRef<T extends Serializable> extends RefImpl<T> implements Serializable {

  private final static Cache<String, GeneratorRef<?>> generatorRefs = Cache.create();
  private final Ref<Generator<T>> generator;
  private transient Ref<T> results = null;

  private GeneratorRef(Ref<Generator<T>> generator) {
    this.generator = generator;

    CompContext.schedule(generator, extractDependencies());

  }

  public static <T extends Serializable, U extends T> GeneratorRef<T> compute(Generator<U> generator) {
    String digest = common.Digest.digest(generator);
    return (GeneratorRef<T>) generatorRefs.get(digest, () -> new GeneratorRef(Ref.ref(generator)));
  }

  @Override
  public T get() {
    ensureResults();
    return results.get();
  }

  private void ensureResults() {
    if (results == null) {
      results = CompContext.getResults(generator);
    }
  }

  @Override
  public String digest() {
    ensureResults();

    return results.digest();
  }


  public ImmutableList<String> extractDependencies() {
    try {
      DependencyOutputStream d = new DependencyOutputStream(new ByteArrayOutputStream());
      d.writeObject(generator.get());
      d.close();

      return ImmutableList.copyOf(d.generatorDependencies);

    } catch (IOException e) {
      throw Exceptions.toRuntime(e);
    }
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    if (out instanceof DependencyOutputStream) {
      ((DependencyOutputStream) out).generatorDependencies.add(generator.digest());
    }
    out.writeObject(this);
  }


}
