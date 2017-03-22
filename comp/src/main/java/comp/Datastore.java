package comp;

import org.omg.CORBA.DataOutputStream;

import java.io.*;
import java.util.function.Function;

/**
 * Created by markneri on 3/21/17.
 */
public class Datastore {
    /*
    Returns something that can serialize very small when using the right output stream


     */
    static <T extends Serializable, U extends T> T store(U toStore)
    {
        return null;
    };

    static abstract class StoreOutputStream extends ObjectOutputStream {

        public StoreOutputStream(OutputStream out) throws IOException {
            super(out);
        }

        public abstract void addDependency(String hash);

    }

    static class HashWithDependency
    {

    }

    static abstract class StoreInputStream extends ObjectInputStream {


        public StoreInputStream(InputStream in) throws IOException {
            super(in);
        }

        <T> T getObject() {return null;}
    }

    static class StoredObject<T extends Serializable> implements Serializable
    {
        String hash;
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
        {
            StoreInputStream storeIn = (StoreInputStream) in;
            hash = in.readUTF();
        }



    }

    static <T extends Serializable> T deserialize(StoreInputStream input)
    {

    }



}
