package common;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


public interface Proxies {

  static Object createProxy(Object object, Handler handler) {
    Class<?> cls = object.getClass();
    return Proxy.newProxyInstance(cls.getClassLoader(), Reflection.getInterfaces(cls), (Serializable & InvocationHandler) (proxy, method, args) ->
        handler.invoke(proxy, object, method, args)
    );
  }

  interface Handler {

    Object invoke(Object proxy, Object baseObject, Method meth0d, Object[] args);
  }
}
