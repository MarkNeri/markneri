package comp.impl;

import comp.Ref;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;


abstract class RefImpl<T extends Serializable> implements Ref<T>, common.Digest.Digestable {

  final public void digest(ObjectOutputStream stream) throws IOException {
    stream.writeUTF(digest());
  }
}
