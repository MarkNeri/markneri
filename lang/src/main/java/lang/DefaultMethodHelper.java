package lang;


import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
                throw Throwables.toRuntime(e);
            }
        }
    }

    private static final Constructor<MethodHandles.Lookup> constructor;

    static {
        try {
            constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
        } catch (NoSuchMethodException e) {
            throw Throwables.toRuntime(e);
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
                throw Throwables.toRuntime(e);
            }
        });

        return methodCache.get(method, () -> {
            try {
                Object methodHandle = classConstructor
                        .unreflectSpecial(method, declaringClass)
                        .bindTo(proxy)
                        .invokeWithArguments(args);
                return (MethodHandle) methodHandle;

            } catch (Throwable e) {
                throw Throwables.toRuntime(e);
            }
        });
    }


}
