package common;


public class Reflection {

  public static Class<?>[] getInterfaces(Class<?> cls) {
    return cls.isInterface() ? new Class<?>[]{cls} : cls.getInterfaces();
  }


}
