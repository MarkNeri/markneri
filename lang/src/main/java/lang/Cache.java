package lang;


import com.google.common.cache.CacheBuilder;

import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by markneri on 3/20/2017.
 */
interface Cache<K, V> {
    public static <K,V> Builder<K, V> builder() {return new Builder<K,V>(){
        @Override
        public Cache<K, V> build() {
            final com.google.common.cache.Cache<K,V> delegate =  CacheBuilder.newBuilder().build();
            return new Cache<K, V>(){

                @Override
                public V get(K k, Function<K, V> f) {
                    return get(k, () -> f.apply(k));
                }

                @Override
                public V get(K k, Supplier<V> f) {
                    try {
                        return delegate.get(k, f::get);
                    } catch (ExecutionException e) {
                        throw Exceptions.toRuntime(e);
                    }
                }
            };
        }
    };}

    static <K,V> Cache<K, V> create() {return Cache.<K,V>builder().build();}

    interface Builder<K, V>
    {
        Cache<K,V> build();
    }

    V get(K k, Function<K, V> f);

    V get(K k, Supplier<V> f);



}
