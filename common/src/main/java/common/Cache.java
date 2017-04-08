package common;


import com.google.common.cache.CacheBuilder;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by mneri on 3/24/17.
 */
public interface Cache<K, V> {

  public static <K, V> Builder<K, V> builder() {

    return new Builder<K, V>() {

      final CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();


      @Override
      public Cache<K, V> build() {

        final com.google.common.cache.Cache<K, V> delegate = builder.build();
        return new Cache<K, V>() {

          @Override
          public V putIfAbsent(K k, V v) {
            return get(k, () -> v);
          }

          @Override
          public V get(K key) {
            V ret = delegate.getIfPresent(key);
            if (ret == null) {
              throw new NoSuchElementException(key.toString());
            }
            return ret;
          }

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

          @Override
          public void ifPresent(K k, Consumer<V> present, Runnable absent) {
            V v = delegate.getIfPresent(k);
            if (v != null) {
              present.accept(v);
            } else {
              absent.run();
            }
          }

          @Override
          public <T> T ifPresent(K k, Function<V, T> present, Supplier<T> absent) {
            V v = delegate.getIfPresent(k);
            if (v != null) {
              return present.apply(v);
            } else {
              return absent.get();
            }
          }

          @Override
          public void ifPresent(K k, Consumer<V> present) {
            ifPresent(k, present, () -> {
            });
          }

          @Override
          public Optional<V> getOptional(K k) {
            return Optional.ofNullable(delegate.getIfPresent(k));
          }
        };
      }

      @Override
      public Builder<K, V> setMaximumSize(int maximumSize) {
        builder.maximumSize(maximumSize);
        return this;
      }
    };
  }

  static <K, V> Cache<K, V> create() {
    return Cache.<K, V>builder().build();
  }

  V putIfAbsent(K k, V v);

  V get(K key);

  V get(K k, Function<K, V> f);

  V get(K k, Supplier<V> f);

  void ifPresent(K k, Consumer<V> present, Runnable absent);

  <T> T ifPresent(K k, Function<V, T> present, Supplier<T> absent);

  void ifPresent(K k, Consumer<V> present);

  Optional<V> getOptional(K k);

  public interface Builder<K, V> {

    Cache<K, V> build();

    Builder<K, V> setMaximumSize(int maximumSize);
  }

}
