package comp.impl;

import common.Cache;
import comp.Ref;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;


public class DataRef<T extends Serializable> extends RefImpl<T> {

  static Cache<String, DataRef<?>> dataRefs = Cache.create();
  private transient T data;
  private String digest;

  DataRef(T data, String digest) {
    CompContext.putData(digest, data);
    this.data = data;
    this.digest = digest;
  }

  DataRef(String digest) {

    this.data = null;
    this.digest = digest;
  }

  public static <T extends Serializable, U extends T> Ref<T> ref(U data) {
    String hash = common.Digest.digest(data);
    return (Ref<T>) dataRefs.get(hash, () -> new DataRef<T>(data, hash));
  }

  static <T extends Serializable> Ref<T> fromDigest(String digest) {
    return new DataRef<T>(digest);
    //return (Ref<T>) dataRefs.get(digest, () -> new DataRef<T>(digest));

  }

  @Override
  public T get() {
    if (data == null) {
      data = CompContext.getData(digest);
    }

    return data;
  }

  @Override
  public String digest() {
    return digest;
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    if (out instanceof DependencyOutputStream) {
      ((DependencyOutputStream) out).dataDependencies.add(digest);
    }
    out.writeObject(this);
  }
}
