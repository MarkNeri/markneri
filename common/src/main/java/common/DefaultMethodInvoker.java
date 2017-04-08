package common;


import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;


public class DefaultMethodInvoker {

  private static final Constructor<MethodHandles.Lookup> constructor;
  private final static Cache<Class<?>, MethodHandles.Lookup> lookupCache = Cache.create();
  private final static Cache<Method, MethodHandle> methodCache = Cache.create();

  static {
    try {
      constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
    } catch (NoSuchMethodException e) {
      throw Exceptions.toRuntime(e);
    }

    if (!constructor.isAccessible()) {
      constructor.setAccessible(true);
    }
  }

  /**
   * Invokes methods properly on interfaces with default implementations
   *
   * @param method The method to invoke
   * @param proxy An instance of an object that implements the interface containing the method
   * @param impl The implementation to invoke non-default methods on
   * @param args The arguments to pass to the method
   * @return The results of invoking the method
   */
  public static Object invokeWithDefault(Method method, Object proxy, Object impl, Object[] args) {
    if (method.isDefault()) {
      return invokeDefaultMethod(method, proxy, args);
    } else {
      try {
        return method.invoke(impl, args);
      } catch (Exception e) {
        throw Exceptions.toRuntime(e);
      }
    }
  }

  private static Object invokeDefaultMethod(Method method, Object proxy, Object[] args) {
    final Class<?> declaringClass = method.getDeclaringClass();

    MethodHandles.Lookup methodLookup = getCachedMethodLookup(declaringClass);

    MethodHandle methodHandle = getCachedMethodHandle(method, declaringClass, methodLookup);

    return invoke(proxy, args, methodHandle);
  }

  private static MethodHandle getCachedMethodHandle(Method method, Class<?> declaringClass, MethodHandles.Lookup methodLookup) {
    return methodCache.get(method, () -> {
      return getMethodHandle(method, declaringClass, methodLookup);
    });
  }

  private static MethodHandles.Lookup getCachedMethodLookup(Class<?> declaringClass) {
    return lookupCache.get(declaringClass, () -> {
      return getMethodLookup(declaringClass);
    });
  }

  private static Object invoke(Object proxy, Object[] args, MethodHandle methodHandle) {
    try {
      return methodHandle.bindTo(proxy)
          .invokeWithArguments(args);
    } catch (Throwable throwable) {
      throw Exceptions.toRuntime(throwable);
    }
  }

  private static MethodHandle getMethodHandle(Method method, Class<?> declaringClass, MethodHandles.Lookup methodLookup) {
    try {
      return methodLookup
          .unreflectSpecial(method, declaringClass);
    } catch (Throwable e) {
      throw Exceptions.toRuntime(e);
    }
  }

  private static MethodHandles.Lookup getMethodLookup(Class<?> declaringClass) {
    try {
      return constructor.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE);
    } catch (Exception e) {
      throw Exceptions.toRuntime(e);
    }
  }


}
