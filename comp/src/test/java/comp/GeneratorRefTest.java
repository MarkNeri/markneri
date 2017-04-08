package comp;

import comp.impl.GeneratorRef;
import java.io.IOException;
import java.util.function.Supplier;
import org.junit.Test;


public class GeneratorRefTest {

  @Test
  public void extactDependencies() throws IOException, ClassNotFoundException {
    Supplier<String> mark = Generator.schedule(() -> "Mark");
    Supplier<String> neri = Generator.schedule(() -> "Neri");

    //GeneratorRef<String> mn = GeneratorRef.compute((Serializable & Generator<String>) () -> mark.get() + neri.get());

    GeneratorRef<String> mn = GeneratorRef.compute(() -> mark.get() + neri.get());
    System.out.println(mn.extractDependencies());

  }
}
