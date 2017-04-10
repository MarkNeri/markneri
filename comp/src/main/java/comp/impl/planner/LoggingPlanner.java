package comp.impl.planner;


import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;

public class LoggingPlanner implements Planner {

  private final Planner delegate;
  private final Logger logger;
  private final AtomicInteger freeMachineId = new AtomicInteger();

  public LoggingPlanner(Planner delegate, Logger logger) {
    this.delegate = delegate;
    this.logger = logger;
  }

  @Override
  public MachineToPlanner connectMachine(final PlannerToMachine machine, InetSocketAddress address) {
    final int machineI = freeMachineId.getAndIncrement();

    PlannerToMachine loggingPM = new PlannerToMachine() {
      @Override
      public void result(String generator, Result result) {
        logger.info("P->M({}): result({}, {})", machineI, generator, result);
        machine.result(generator, result);
      }

      @Override
      public void fetchData(String digest, InetSocketAddress address) {
        logger.info("P->M({}): fetchData({}, {})", machineI, digest, address);
        machine.fetchData(digest, address);
      }

      @Override
      public void flushed() {
        logger.debug("P->M({}): flushed()", machineI);
        machine.flushed();
      }


    };

    final MachineToPlanner machineDelegate = delegate.connectMachine(loggingPM, address);

    final AtomicInteger threadId = new AtomicInteger();

    return new MachineToPlanner() {

      @Override
      public void fetchComplete(String digest, InetSocketAddress address, boolean success) {
        logger.info("M({})->P: fetchComplete({}. {}, {})", machineI, digest, address, success);
        machineDelegate.fetchComplete(digest, address, success);
      }

      @Override
      public void machineDead() {
        logger.info("M({})->P: machineDied()", machineI);
        machineDelegate.machineDead();
      }

      @Override
      public void flush() {
        logger.debug("M({})->P: flush()", machineI);
        machineDelegate.flush();
      }


      @Override
      public ThreadToPlanner connectThread(final PlannerToThread thread) {
        final int threadI = threadId.getAndIncrement();

        logger.info("M({})->P: connectThread({})", machineI, threadI);

        final PlannerToThread loggingPT = new PlannerToThread() {
          @Override
          public void execute(String generatorDigest) {
            logger.info("P->T({}, {}): execute({})", machineI, threadI, generatorDigest);
            thread.execute(generatorDigest);
          }

          @Override
          public void proceed() {
            logger.info("P->T({}, {}): proceed()", machineI, threadI);
            thread.proceed();
          }

          @Override
          public void abort() {
            logger.info("P->T({}, {}): abort()", machineI, threadI);
            thread.abort();
          }
        };

        final ThreadToPlanner threadDelegate = machineDelegate.connectThread(loggingPT);

        return new ThreadToPlanner() {
          @Override
          public void scheduleGenerator(String generator, Iterable<String> generatorDependencies) {
            logger.info("T({}, {})->P: schedule({})", machineI, threadI, generator);
            threadDelegate.scheduleGenerator(generator, generatorDependencies);
          }

          @Override
          public void threadIdle() {
            logger.info("T({}, {})->P: threadIdle()", machineI, threadI);
            threadDelegate.threadIdle();

          }

          @Override
          public void resultNeeded(String generatorDigest) {
            logger.info("T({}, {})->P: resultNeeded({})", machineI, threadI, generatorDigest);
            threadDelegate.resultNeeded(generatorDigest);
          }

          @Override
          public void dataNeeded(String dataDigest) {
            logger.info("T({}, {})->P: dataNeeded({})", machineI, threadI, dataDigest);
            threadDelegate.dataNeeded(dataDigest);
          }

          @Override
          public void executionComplete(String generator, Result result) {
            logger.info("T({}, {})->P: complete({}, {})", machineI, threadI, generator, result);
            threadDelegate.executionComplete(generator, result);
          }

          @Override
          public void dataAvailable(String digest) {
            logger.info("T({}, {})->P: dataAvailable({})", machineI, threadI, digest);
            threadDelegate.dataAvailable(digest);
          }
        };
      }

    };

  }


}
