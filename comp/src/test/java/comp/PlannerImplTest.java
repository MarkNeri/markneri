package comp;

import static org.mockito.Mockito.*;


import ch.qos.logback.classic.Level;
import com.google.common.collect.Queues;
import com.google.common.collect.ImmutableList;
import common.Exceptions;
import comp.impl.planner.LoggingPlanner;
import comp.impl.planner.Planner;
import comp.impl.planner.Planner.PlannerToThread;
import comp.impl.planner.Planner.Result;
import comp.impl.planner.Planner.ThreadToPlanner;
import comp.impl.planner.PlannerImpl;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class PlannerImplTest {


  @Test
  public void test() {
    Planner p = new PlannerImpl();
    p = new LoggingPlanner(p, LoggerFactory.getLogger("Test"));

    Machine m = new Machine(p);

    Thread main = new Thread(m);
    Thread worker = new Thread(m);

    main.scheduleGenerator("g", ImmutableList.of());

    //Nothing should execute yet because the main thread isn't blocked and the worker hasn't indicate that it's idle
    verify(worker.mock, never()).execute(any());
    verify(main.mock, never()).execute(any());

    worker.threadIdle();

    //verify(m.mock, timeout(1000)).flushed();

    //The generator should get assigned to he worker
    worker.verify().execute("g");

    main.resultNeeded("g");
    worker.executionComplete("g", new Planner.Result("a", null, null));

    m.flush();

    verify(main.mock).proceed();

  }

  @Test
  public void justMain() {
    Planner p = new PlannerImpl();
    justMainImpl(p);
  }

  @Test
  public void justMainWithLogging() {
    Planner p = new PlannerImpl();
    justMainImpl(new LoggingPlanner(p, LoggerFactory.getLogger("Test")));

  }

  private void justMainImpl(Planner p) {
    Machine m = new Machine(p);

    Thread thread = new Thread(m);

    thread.scheduleGenerator("g", ImmutableList.of());

    //Nothing should execute yet because the main thread isn't blocked and the worker hasn't indicate that it's idle
    verify(thread.mock, never()).execute(any());

    m.flush();

    thread.resultNeeded("g");

    m.flush();

    verify(thread.mock).execute("g");

    thread.executionComplete("g", new Planner.Result("a", null, null));

    m.flush();

    verify(thread.mock).proceed();
  }

  @Test
  public void twoMachines() {
    Planner p = new PlannerImpl();
    p = new LoggingPlanner(p, LoggerFactory.getLogger("Test"));

    Machine m1 = new Machine(p);

    Thread thread1 = new Thread(m1);

    thread1.dataAvailable("ga");
    thread1.scheduleGenerator("ga", ImmutableList.of());



    Machine m2 = new Machine(p);

    Thread thread2 = new Thread(m2);

    thread2.threadIdle();


    verify(thread2.mock).execute("ga");

    thread2.dataNeeded("ga");

    m2.verify().fetchData("ga", m1.address);
    m2.fetchComplete("ga", m1.address, true);

    thread2.verify().proceed();

    thread2.executionComplete("ga", new Result("gar", null, null));

    thread1.resultNeeded("ga");
    m1.verify().result("ga", new Result("gar", null, null));
    thread1.verify().proceed();


  }

  @Test
  public void twoMachinesWithFailureAndTakeoverByMain() throws InterruptedException {
    Planner p = new PlannerImpl();
    p = new LoggingPlanner(p, LoggerFactory.getLogger("Test"));

    Machine m1 = new Machine(p);

    Thread thread1 = new Thread(m1);

    thread1.dataAvailable("ga");
    thread1.scheduleGenerator("ga", ImmutableList.of());


    Machine m2 = new Machine(p);

    Thread thread2 = new Thread(m2);

    thread2.threadIdle();

    verify(thread2.mock).execute("ga");

    thread1.resultNeeded("ga");

    thread2.dataNeeded("ga");

    m2.verify().fetchData("ga", m1.address);
    m2.fetchComplete("ga", m1.address, true);

    thread2.verify().proceed();

    m2.machineDead();
    m2.flush();

    verify(thread1.mock).execute("ga");
    thread1.executionComplete("ga", new Result("gar", null, null));

    m1.verify().result("ga", new Result("gar", null, null));
    thread1.verify().proceed();


  }

  @Test
  public void threeMachinesWithFailureAndTakeoverbyWorker() throws InterruptedException {
    Planner p = new PlannerImpl();
    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("Test");
    root.setLevel(Level.INFO);

    p = new LoggingPlanner(p, LoggerFactory.getLogger("Test"));

    Machine m1 = new Machine(p);

    Thread thread1 = new Thread(m1);

    thread1.dataAvailable("ga");
    thread1.scheduleGenerator("ga", ImmutableList.of());

    Machine m2 = new Machine(p);

    Thread thread2 = new Thread(m2);

    thread2.threadIdle();

    thread2.verify().execute("ga");

    Machine m3 = new Machine(p);
    Thread thread3 = new Thread(m3);
    thread3.threadIdle();
    m3.flush();

    thread2.dataNeeded("ga");

    m2.verify().fetchData("ga", m1.address);
    m2.fetchComplete("ga", m1.address, true);

    thread2.verify().proceed();

    m2.machineDead();
    m2.flush();
    m3.flush();
    thread3.verify().execute("ga");
    //Faking the data interchange here.  The Planner could complain someday
    thread3.executionComplete("ga", new Result("gar", null, null));
    m3.flush();

    thread1.resultNeeded("ga");
    m1.verify().result("ga", new Result("gar", null, null));
    thread1.verify().proceed();


  }


  static class Thread implements Planner.ThreadToPlanner {

    Thread(Machine m) {
      this.machine = m;
      mock = mock(Planner.PlannerToThread.class);
      toPlannerPrivate = m.connectThread(mock);
    }


    final Planner.PlannerToThread mock;
    final Planner.ThreadToPlanner toPlannerPrivate;
    final Machine machine;

    Planner.PlannerToThread verify() {
      machine.flush();
      return Mockito.verify(mock);
    }

    void flush() {
      machine.flush();
    }



    @Override
    public void threadIdle() {
      toPlannerPrivate.threadIdle();
      flush();
    }

    @Override
    public void resultNeeded(String generatorDigest) {
      toPlannerPrivate.resultNeeded(generatorDigest);
      flush();
    }

    @Override
    public void dataNeeded(String dataDigest) {
      toPlannerPrivate.dataNeeded(dataDigest);
      flush();

    }

    @Override
    public void scheduleGenerator(String generator, Iterable<String> generatorDependencies) {
      toPlannerPrivate.scheduleGenerator(generator, generatorDependencies);
      flush();

    }

    @Override
    public void executionComplete(String generator, Result result) {
      toPlannerPrivate.executionComplete(generator, result);
      flush();


    }

    @Override
    public void dataAvailable(String digest) {
      toPlannerPrivate.dataAvailable(digest);
      flush();
    }
  }


  static class Machine implements Planner.PlannerToMachine, Planner.MachineToPlanner {

    Machine(Planner p) {

      address = new InetSocketAddress("127.0.0.1", freePort++);
      mock = spy(this);
      toPlannerPrivate = p.connectMachine(mock, address);

    }

    private static int freePort = 0;

    private final Planner.MachineToPlanner toPlannerPrivate;
    final Planner.PlannerToMachine mock;
    final InetSocketAddress address;


    Planner.PlannerToMachine verify() {
      flush();
      return Mockito.verify(mock);
    }

    private BlockingQueue q = Queues.newLinkedBlockingQueue();

    @Override
    public void result(String generator, Planner.Result result) {

    }

    @Override
    public void fetchData(String digest, InetSocketAddress address) {

    }

    @Override
    public void flushed() {
      q.add(this);
    }


    @Override
    public ThreadToPlanner connectThread(PlannerToThread worker) {
      ThreadToPlanner ret = toPlannerPrivate.connectThread(worker);
      flush();
      return ret;

    }

    @Override
    public void fetchComplete(String digest, InetSocketAddress address, boolean success) {
      toPlannerPrivate.fetchComplete(digest, address, success);
      flush();
    }

    @Override
    public void machineDead() {
      toPlannerPrivate.machineDead();
      flush();
    }

    public void flush() {
      toPlannerPrivate.flush();
      try {
        q.take();

      } catch (InterruptedException e) {
        throw Exceptions.toRuntime(e);
      }
    }

  }
}
