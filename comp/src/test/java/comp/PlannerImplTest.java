package comp;

import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import common.Exceptions;
import comp.impl.planner.LoggingPlanner;
import comp.impl.planner.Planner;
import comp.impl.planner.PlannerImpl;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class PlannerImplTest {

  @Test
  public void test() {
    Planner p = new PlannerImpl();

    Machine m = new Machine();
    Planner.PlannerToMachine testMachine = spy(m);
    Planner.MachineToPlanner toPlanner = p.connectMachine(testMachine, new InetSocketAddress("a", 13));
    m.toPlanner = toPlanner;

    Planner.PlannerToThread mainThread = mock(Planner.PlannerToThread.class);
    Planner.ThreadToPlanner mainToPlanner = toPlanner.connectThread(mainThread);

    Planner.PlannerToThread workerThread = mock(Planner.PlannerToThread.class);
    Planner.ThreadToPlanner workerToPlanner = toPlanner.connectThread(workerThread);

    mainToPlanner.scheduleGenerator("g", ImmutableList.of());

    //Nothing should execute yet because the main thread isn't blocked and the worker hasn't indicate that it's idle
    verify(workerThread, never()).execute(any());
    verify(mainThread, never()).execute(any());

    workerToPlanner.threadIdle();
    m.flush();

    //The generator should get assigned to he worker
    verify(workerThread).execute("g");

    mainToPlanner.resultNeeded("g");
    workerToPlanner.executionComplete("g", new Planner.Result("a", null, null));

    m.flush();

    verify(mainThread).proceed();

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
    Machine m = new Machine();
    Planner.PlannerToMachine testMachine = spy(m);
    Planner.MachineToPlanner toPlanner = p.connectMachine(testMachine, new InetSocketAddress("a", 13));
    m.toPlanner = toPlanner;

    Planner.PlannerToThread mainThread = mock(Planner.PlannerToThread.class);
    Planner.ThreadToPlanner mainToPlanner = toPlanner.connectThread(mainThread);

    mainToPlanner.scheduleGenerator("g", ImmutableList.of());

    //Nothing should execute yet because the main thread isn't blocked and the worker hasn't indicate that it's idle
    verify(mainThread, never()).execute(any());

    m.flush();

    mainToPlanner.resultNeeded("g");

    m.flush();

    verify(mainThread).execute("g");

    mainToPlanner.executionComplete("g", new Planner.Result("a", null, null));

    m.flush();

    verify(mainThread).proceed();
  }

  static class Machine implements Planner.PlannerToMachine {

    BlockingQueue q = Queues.newLinkedBlockingQueue();
    Planner.MachineToPlanner toPlanner;

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

    public void waitForFlushed() {

    }

    void flush() {
      toPlanner.flush();
      try {
        q.take();
      } catch (InterruptedException e) {
        throw Exceptions.toRuntime(e);
      }
    }

  }
}
