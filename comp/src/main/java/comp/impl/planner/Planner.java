package comp.impl.planner;

import java.io.Serializable;
import java.net.InetSocketAddress;


public interface Planner {

  MachineToPlanner connectMachine(PlannerToMachine machine, InetSocketAddress address);

  interface ThreadToPlanner {

    void threadIdle();

    void resultNeeded(String generatorDigest);

    void dataNeeded(String dataDigest);


    void scheduleGenerator(String generator, Iterable<String> generatorDependencies);

    void executionComplete(String generator, Result result);

    void dataAvailable(String digest);
  }

  interface PlannerToThread {

    void execute(String generatorDigest);

    void proceed();

    void abort();
  }

  interface PlannerToMachine {

    void result(String generator, Result result);

    void fetchData(String digest, InetSocketAddress address);

    void flushed();

  }

  interface MachineToPlanner {

    ThreadToPlanner connectThread(PlannerToThread worker);

    void fetchComplete(String digest, InetSocketAddress address, boolean success);

    void machineDead();

    void flush();
  }

  static class Result implements Serializable {

    public final String result;
    public final String exception;
    public final String log;

    public Result(String result, String exception, String log) {
      this.result = result;
      this.exception = exception;
      this.log = log;
    }

    @Override
    public String toString() {
      return "Result{" +
          "result='" + result + '\'' +
          ", exception='" + exception + '\'' +
          ", log='" + log + '\'' +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Result result1 = (Result) o;

      if (result != null ? !result.equals(result1.result) : result1.result != null) {
        return false;
      }
      if (exception != null ? !exception.equals(result1.exception) : result1.exception != null) {
        return false;
      }
      return log != null ? log.equals(result1.log) : result1.log == null;
    }

    @Override
    public int hashCode() {
      int result1 = result != null ? result.hashCode() : 0;
      result1 = 31 * result1 + (exception != null ? exception.hashCode() : 0);
      result1 = 31 * result1 + (log != null ? log.hashCode() : 0);
      return result1;
    }
  }


}
