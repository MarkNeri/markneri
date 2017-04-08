package comp;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.function.Supplier;
import org.junit.Test;


public class ObjectStoreTest {

  @Test
  public void computeTest() throws IOException, ClassNotFoundException {

    List<Supplier<String>> rets = Lists.newArrayList();
    for (int i = 0; i < 3; i++) {
      int ii = i;
      rets.add(Generator.schedule(() ->
      {
        try {
          Thread.sleep(1000);
          if (System.currentTimeMillis() % 2 == 0) {
            throw new RuntimeException("adfa");
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        return ii + " " + Thread.currentThread();
      }));

    }
        /*
        Supplier<String> mark = Generator.schedule(() ->
        {
            try {
                Worker.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "Mark" + Worker.currentThread();
        });

        new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(mark);


        Supplier<String> neri = Generator.schedule(() -> "Neri" + Worker.currentThread());

        */

    //Supplier<String> mn = Generator.schedule(() -> mark.get() + neri.get());

    //System.out.println(mn.get());
    for (Supplier<String> r : rets) {
      System.out.println(r.get());
    }


  }

  interface I extends Serializable {

    double d();
  }

  static class II implements I {

    private final double v;

    public II(double v) {
      this.v = v;
    }

    @Override
    public double d() {
      return v;
    }
  }


}
