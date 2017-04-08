package tables;


import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.Set;
import lang.DefaultMethodHelper;

/**
 * Created by ubuntu on 3/19/17.
 */
public interface Table<T extends Table> {

  static <T extends Table> T from(Class<T> cls, Table table) {

    ImmutableMap.Builder<String, Column> columnsBuilder = ImmutableMap.builder();

    for (Method m : cls.getMethods()) {
      if ((m.getModifiers() & Modifier.ABSTRACT) != 0) {
        if (Column.class.isAssignableFrom(m.getReturnType())) {
          String name = m.getName();
          columnsBuilder.put(name, table.column(name));
        }
      }
    }
    ImmutableMap<String, Column> namedColumns = columnsBuilder.build();

    return (T) Proxy.newProxyInstance(cls.getClassLoader(),
        new Class<?>[]{cls}, (proxy, method, args) ->
        {
          if (namedColumns.containsKey(method.getName())) {
            return namedColumns.get(method.getName());
          } else {
            return DefaultMethodHelper.invokeWithDefault(method, proxy, table, args);
          }
        });
  }

  static <T extends Table> T from(Class<T> cls, Array... columns) {

    ImmutableMap.Builder<String, Column> columnsBuilder = ImmutableMap.builder();

    //Get the param name order in the classes create function
    for (Method m : cls.getMethods()) {

      if (m.getName().contentEquals("from")) {
        final Parameter[] parameters = m.getParameters();

        if (Iterables.all(ImmutableList.copyOf(parameters), p -> p.getType() == Array.class)) {

          for (int i = 0; i < columns.length; i++) {
            String name = parameters[i].getName();
            if (name.startsWith("arg")) {
              throw new RuntimeException("Needs to be compiled with the parameters option");
            }
            columnsBuilder.put(name, Column.from(name, columns[i]));
          }
          break;
        }
      }
    }
    ImmutableMap<String, Column> namedColumns = columnsBuilder.build();

    Impl impl = new Impl(ImmutableList.copyOf(namedColumns.values()));

    return (T) Proxy.newProxyInstance(cls.getClassLoader(),
        new Class<?>[]{cls}, (proxy, method, args) ->
        {
          if (namedColumns.containsKey(method.getName())) {
            return namedColumns.get(method.getName());
          } else {
            return DefaultMethodHelper.invokeWithDefault(method, proxy, impl, args);
          }
        });
  }

  static Table from(Column... columns) {
    return new Impl(ImmutableList.copyOf(columns));
  }

  ImmutableList<Column> columns();

  public default Column column(String name) {
    return Iterables.find(columns(), c -> c.name().contentEquals(name));
  }

  public default int numColumns() {
    return columns().size();
  }

  public default void print() {
    for (Column c : columns()) {
      System.out.println(c.name());
    }
  }

  default Table without(String name) {
    Iterable<Column> filter = Iterables.filter(columns(), c -> !c.name().contentEquals(name));

    return new Impl(ImmutableList.copyOf(filter));
  }

  public default T with(Column c) {
    ImmutableList.Builder<Column> columns = ImmutableList.builder();
    columns.addAll(columns());
    columns.add(c);

    Impl impl = new Impl(columns.build());

    Class<? extends Table> cls = getClass();

    return (T) Proxy.newProxyInstance(cls.getClassLoader(),
        cls.getInterfaces(), (proxy, method, args) ->
        {
          if (method.getDeclaringClass() == Impl.class || method.getDeclaringClass() == Table.class) {
            return DefaultMethodHelper.invokeWithDefault(method, proxy, impl, args);
          } else {
            return DefaultMethodHelper.invokeWithDefault(method, proxy, this, args);
          }

        });
  }

  static class Impl implements Table {

    private final ImmutableList<Column> columns;

    Impl(Iterable<Column> columnsIn) {
      ImmutableList<Column> columns = ImmutableList.copyOf(columnsIn);

      if (columns.size() > 0) {
        Set<String> seen = Sets.newHashSet();
        Column first = columns.get(0);
        for (Column c : columns) {
          Preconditions.checkArgument(c.length() == first.length(), "Length mismatch");
          boolean added = seen.add(c.name());
          Preconditions.checkArgument(added, "Duplicate column " + c.name());
        }
      }

      this.columns = columns;
    }

    @Override
    public ImmutableList<Column> columns() {
      return columns;
    }
  }


}
