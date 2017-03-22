package lang;

import java.util.concurrent.ExecutionException;

/**
 * Created by markneri on 3/20/2017.
 */
public class Throwables {
    public static RuntimeException toRuntime(Throwable t)
    {
        if (t instanceof ExecutionException)
        {
            return toRuntime(t.getCause());
        }
        else if (t instanceof RuntimeException)
        {
            return (RuntimeException) t;
        }
        else
        {
            return new RuntimeException(t);
        }
    }
}
