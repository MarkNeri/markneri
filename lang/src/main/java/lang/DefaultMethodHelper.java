package lang;


import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Created by ubuntu on 3/19/17.
 */
public class DefaultMethodHelper {

    public static Object invokeWithDefault(Method method, Object proxy, Object impl, Object[] args) {
        if (method.isDefault()) {
            return invokeDefaultMethod(method, proxy, args);
        }
        else {
            try {
                return method.invoke(impl, args);
            } catch (Exception e) {
                throw Exceptions.toRuntime(e);
            }
        }
    }

    private static final Constructor<MethodHandles.Lookup> constructor;

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

    private final static Cache<Class<?>, MethodHandles.Lookup> lookupCache = Cache.create();
    private final static Cache<Method, MethodHandle> methodCache = Cache.create();

    private static Object invokeDefaultMethod(Method method, Object proxy, Object[] args) {
        final Class<?> declaringClass = method.getDeclaringClass();

        MethodHandles.Lookup classConstructor = lookupCache.get(declaringClass, () -> {
            try {
                return constructor.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE);
            } catch (Exception e) {
                throw Exceptions.toRuntime(e);
            }
        });

        MethodHandle methodHandle = methodCache.get(method, () -> {
            try {
                return classConstructor
                        .unreflectSpecial(method, declaringClass);


            } catch (Throwable e) {
                throw Exceptions.toRuntime(e);
            }
        });


        try {
            return methodHandle.bindTo(proxy)
                    .invokeWithArguments(args);
        } catch (Throwable throwable) {
            throw Exceptions.toRuntime(throwable);
        }
    }


}
