package comp.impl;

import common.Cache;
import common.CommandQueue;
import common.Exceptions;
import common.ThreadLocals;
import comp.Generator;
import comp.Ref;
import comp.impl.planner.ImmutableResult;
import comp.impl.planner.LoggingPlanner;
import comp.impl.planner.Planner;
import comp.impl.planner.PlannerClient;
import comp.impl.planner.PlannerImpl;
import comp.impl.planner.PlannerServer;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.LoggerFactory;


class CompContext implements Planner.PlannerToThread {


  static PlannerServer server;
  private static Supplier<CompContext> compContexts = ThreadLocals.perThread(() -> {

    return new CompContext();
  });
  private static Cache<String, Planner.Result> results = Cache.create();

  static {
    Planner planner = new PlannerImpl();
    planner = new LoggingPlanner(planner, LoggerFactory.getLogger("server"));

    server = new PlannerServer(planner);

    //planner = new PlannerClient(InetAddress.getLoopbackAddress(), server.getPort());
    // }

    //planner = new LoggingPlanner(planner, System.out);

  }

  static {
    for (int i = 0; i < 3; i++) {
      new Thread(() -> {
        CompContext compContext = compContexts.get();
        compContext.threadToPlanner.threadIdle();
        while (true) {
          compContext.commandQueue.step();
        }

      }).start();
    }

  }

  final Planner.ThreadToPlanner threadToPlanner;
  CommandQueue.Steppable commandQueue = CommandQueue.createSteppable();
  boolean canProceed = true;
  Machine machine = new Machine(new PlannerClient(new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getPort())));


  private CompContext() {
    threadToPlanner = machine.planner.connectThread(this);


  }

  public static <T extends Serializable> T getData(String digest) {

    return compContexts.get().getDataI(digest);
  }

  public static <T extends Serializable> void schedule(Ref<Generator<T>> generator, Iterable<String> dependencies) {

    compContexts.get().threadToPlanner.scheduleGenerator(generator.digest(), dependencies);
  }

  public static <T extends Serializable> Ref<T> getResults(Ref<Generator<T>> generator) {
    return compContexts.get().getResultsI(generator);
  }

  public static void putData(String hash, Serializable toStore) {
    CompContext compContext = compContexts.get();

    compContext.machine.dataStore.putData(hash, toStore);
    compContext.threadToPlanner.dataAvailable(hash);
  }

  @Override
  public void execute(String generatorDigest) {
    commandQueue.add(() -> {

      Ref<Generator<?>> ref = DataRef.fromDigest(generatorDigest);
      Generator<?> g = ref.get();
      Planner.Result result;
      try {
        Serializable r = g.get();
        Ref<Serializable> retRef = Ref.ref(r);
        result = ImmutableResult.builder().result(retRef.digest()).build();

      } catch (Exception e) {
        result = ImmutableResult.builder().exception(Ref.ref(e).digest()).build();
      }

      threadToPlanner.executionComplete(generatorDigest, result);

    });
  }

  @Override
  public void proceed() {

    commandQueue.add(() -> {
      canProceed = true;
    });
  }

  @Override
  public void abort() {

    commandQueue.add(() -> {
      throw new RuntimeException("Aborted");
    });
  }

  ;

  private <T extends Serializable> T getDataI(String digest) {
    DataStore ds = machine.dataStore;
    while (true) {
      Optional<Serializable> opt = ds.getObject(digest);
      if (opt.isPresent()) {
        return (T) opt.get();
      }

      threadToPlanner.dataNeeded(digest);
      processUntilProceed();
    }
  }

  private <T extends Serializable> Ref<T> getResultsI(Ref<Generator<T>> generator) {
    String generatorDigest = generator.digest();

    while (true) {
      try {

        Planner.Result result = results.get(generatorDigest);
        if (result.result().isPresent()) {
          return DataRef.fromDigest(result.result().get());
        } else {
          final Ref<Exception> e = DataRef.fromDigest(result.exception().get());
          return new Ref<T>() {
            @Override
            public T get() {
              throw Exceptions.toRuntime(e.get());
            }

            @Override
            public String digest() {
              throw Exceptions.toRuntime(e.get());
            }
          };

        }
      } catch (NoSuchElementException e) {
        threadToPlanner.resultNeeded(generatorDigest);

        processUntilProceed();
      }
    }
  }

  private void processUntilProceed() {
    canProceed = false;
    while (!canProceed) {
      commandQueue.step();
    }
    canProceed = false;
  }

  static class Machine implements Planner.PlannerToMachine {

    Planner.MachineToPlanner planner;

    DataStore dataStore = new DataStore();

    Machine(Planner planner) {
      this.planner = planner.connectMachine(this, dataStore.getSocketAddress());
    }

    @Override
    public void result(String generator, Planner.Result result) {

      results.putIfAbsent(generator, result);
    }

    @Override
    public void fetchData(String digest, InetSocketAddress address) {
      Optional<Object> opt = dataStore.fetchData(digest, address);

      planner.fetchComplete(digest, address, opt.isPresent());
    }

    @Override
    public void flushed() {

    }
  }


}
