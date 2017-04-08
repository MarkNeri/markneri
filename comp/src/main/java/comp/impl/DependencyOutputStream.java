package comp.impl;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Set;


class DependencyOutputStream extends ObjectOutputStream {


  Set<String> generatorDependencies = Sets.newHashSet();
  Set<String> dataDependencies = Sets.newHashSet();
  public DependencyOutputStream(OutputStream outputStream) throws IOException {
    super(outputStream);
  }
}
