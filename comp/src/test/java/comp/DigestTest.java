package comp;

import common.Digest;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.junit.Test;

public class DigestTest {

  @Test
  public void test() {
    System.out.println(Digest.digest(7));

    System.out.println(Digest.digest(new Data(7)));

  }

  static class Data<T extends Serializable> implements common.Digest.Digestable {

    private final T t;

    Data(T t) {
      this.t = t;
    }

    @Override
    public void digest(ObjectOutputStream digestOutputStream) throws IOException {
      digestOutputStream.writeObject(t);
    }
  }
}
