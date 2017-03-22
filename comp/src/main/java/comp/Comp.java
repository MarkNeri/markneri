package comp;

import org.omg.CORBA.DataOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Created by markneri on 3/18/2017.
 */
public interface Comp {

    ExecutorService exec = Executors.newFixedThreadPool(4);

    static <T extends Serializable, U extends T> T comp(Supplier<? extends U> supplier, Class<U> ...cls)
    {
        return (T) Proxy.newProxyInstance(supplier.getClass().getClassLoader(), cls, (InvocationHandler & Serializable) (object, method, args) -> {
            return method.invoke(exec.submit(supplier::get).get(), args);
        });
    };

    static <T extends Serializable, U extends T> T data(U data)
    {
         InvocationHandler invocationHandler = (InvocationHandler & Serializable)(object, method, args) -> {


            return method.invoke(data, args);
        };
        return (T) Proxy.newProxyInstance(data.getClass().getClassLoader(), data.getClass().getInterfaces(), invocationHandler);
    }



    static class DataImpl<T> implements Serializable
    {
        /*
         Serialize just the hash of the data, when we a dependency aware serializer

         When we deserialize, just deserialize the hash and return a lazy proxy object
         */

        T data;
    }


    static interface Name extends Serializable
    {
        default String first() {return "defaultFirst";};
        String last();
        String middle();
        long createThreadId();
    }

    static class NameImpl implements Name {
        @Override
        public long createThreadId() {
            return createThreadId;
        }

        private final long createThreadId;

        public NameImpl(String first, String middle, String last) {
            this.first = first;
            this.middle = middle;
            this.last = last;
            this.createThreadId = Thread.currentThread().getId();
        }

        final String first;
        final String middle;
        final String last;

        //@Override
        //public String first() {
//            return first;
 //       }

        @Override
        public String last() {
            return last;
        }

        @Override
        public String middle() {
            return middle;
        }


    }

    public static void main(String[] args) throws IOException {
        Name a = data(new NameImpl("mark", "l", "neri"));
        Name b = comp(() -> new NameImpl("mark", "l", "neri"), Name.class);
        Name c = comp(() -> new NameImpl("mark", "l", "neri"), Name.class);


        ObjectOutputStream s = new ObjectOutputStream(new ByteArrayOutputStream());

        s.writeObject(a);
        s.writeObject(b);
        s.writeObject(c);

        System.out.println(a.first() + " " + a.createThreadId());
        System.out.println(b.first() + " " + b.createThreadId());
        System.out.println(c.first() + " " + c.createThreadId());



    }
}
