package comp.impl.planner;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Optional;
import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;


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

  @Value.Immutable
  interface GeneratorInfo extends Serializable
  {
      String generator();
      Iterable<String> dependencies();
      Iterable<String> locality();
      Optional<String> merger(); //The digest of a class that can take this generator along with other and compute them all together efficiently
  }

  @Value.Immutable
  interface Result extends Serializable {

    public Optional<String> result();
    public Optional<String> exception();
    public Optional<String> log();


  }


}
