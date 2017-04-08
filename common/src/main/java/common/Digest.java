package common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import org.apache.commons.codec.digest.DigestUtils;


public class Digest {

  public static String digest(Object o) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try {
      DigestOutputStream digestOutputStream = new DigestOutputStream(bytes);
      digestOutputStream.writeObject(o);
      digestOutputStream.close();

      return DigestUtils.sha512Hex(bytes.toByteArray());
    } catch (IOException e) {
      throw Exceptions.toRuntime(e);

    }
  }


  public interface Digestable {

    void digest(ObjectOutputStream digestOutputStream) throws IOException;
  }

  private static class DigestOutputStream extends ObjectOutputStream {

    ObjectOutputStream delegate;

    DigestOutputStream(OutputStream output) throws IOException {
      super();
      delegate = new ObjectOutputStream(output) {
        @Override
        protected void writeStreamHeader() throws IOException {

        }

        @Override
        protected void writeClassDescriptor(ObjectStreamClass objectStreamClass) throws IOException {

        }
      };
    }

    @Override
    protected void writeObjectOverride(Object o) throws IOException {
      if (o instanceof Digestable) {
        ((Digestable) o).digest(this);
      } else {
        delegate.writeObject(o);
      }
    }

    @Override
    public void close() throws IOException {
      delegate.close();
    }
  }
}