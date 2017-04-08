package comp.impl.planner;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import common.CommandQueue;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;


public class PlannerImpl implements Planner {


  final LinkedList<Task> unstartedTasks = Lists.newLinkedList();
  final Map<String, Task> tasks = Maps.newHashMap();
  final Map<String, Data> data = Maps.newHashMap();
  final Set<Machine> machines = Sets.newHashSet();
  private final CommandQueue commands = CommandQueue.createDaemon("Planner");

  void check() {
    tasks.forEach((digest, task) -> {
      Preconditions.checkState(task.generator.equals(digest));
      Preconditions.checkState(task.hasStarted != unstartedTasks.contains(task));

      if (task.result != null) {
        Preconditions.checkState(task.hasStarted);
      }

    });


  }

  void schedule() {
    if (commands.getIsFlushing()) {
      return;
    }

    //Let anything that's done proceed
    for (Machine machine : machines) {
      for (Worker thread : machine.threads) {

        //If the thread is blocked, check that it if we can unblock it.
        String blockingGenerator = getBlockingGenerator(thread);
        if (blockingGenerator != null) {
          Task blockingTask = tasks.get(blockingGenerator);
          if (blockingTask.result != null) {
            machine.remote.result(blockingTask.generator, blockingTask.result);
            thread.remote.proceed();
            clearBlockingGenerator(thread);
          }
        }

        String blockingData = getBlockingData(thread);
        if (blockingData != null) {
          Data d = data.get(blockingData);
          if (d.machines.contains(machine)) {
            thread.remote.proceed();
            clearBlockingData(thread);
          }
        }
      }
    }

    for (Machine machine : machines) {
      for (Worker thread : machine.threads) {
        //There isn't a pending fetch data request
        String blockingGenerator = getBlockingGenerator(thread);
        if (blockingGenerator != null) {
          //We are waiting for the results of another generator
          Task newTask = tasks.get(blockingGenerator);
          if (!newTask.hasStarted) {
            unstartedTasks.remove(newTask);
            newTask.hasStarted = true;
            thread.remote.execute(newTask.generator);
            thread.stack.add(newTask);
          } else {
            //Just wait patiently for now while another thread finishes the work
          }
        } else {
          String blockingData = getBlockingData(thread);
          if (blockingData != null) {
            if (machine.pendingData == null) {
              //We are waiting on data, and don't have a fetch request outstanding
              Data d = data.get(blockingData);
              Machine m = Iterables.getFirst(d.machines, null);
              machine.remote.fetchData(blockingData, m.address);
              machine.pendingData = blockingData;
            } else {
              //There is an outstanding fetch request for the data
            }

          } else {
            if (thread.isWorker && thread.stack.isEmpty()) {
              if (!unstartedTasks.isEmpty()) {
                Task toRun = unstartedTasks.removeLast();
                toRun.hasStarted = true;
                thread.remote.execute(toRun.generator);
                thread.stack.add(toRun);
              }
            } else {
              //Don't scedhule work to the main thread unless it is waiting
            }

          }
        }
      }
    }
  }

  private void clearBlockingData(Worker thread) {
    if (thread.stack.isEmpty()) {
      thread.topWaitingForData = null;
    } else {
      thread.stack.getLast().blockingData = null;
    }
  }

  private void clearBlockingGenerator(Worker thread) {
    if (thread.stack.isEmpty()) {
      thread.topWaitingForGenerator = null;
    } else {
      thread.stack.getLast().blockingGenerator = null;
    }
  }

  private String getBlockingGenerator(Worker thread) {
    return thread.stack.isEmpty() ? thread.topWaitingForGenerator
        : thread.stack.getLast().blockingGenerator;
  }

  private String getBlockingData(Worker thread) {
    return thread.stack.isEmpty() ? thread.topWaitingForData : thread.stack.getLast().blockingData;
  }

  void idleThread(Worker plannedThread) {
    plannedThread.isWorker = true;
    schedule();
  }

  void scheduleGenerator(Worker thread, String generator,
      Iterable<String> generatorDependencies) {

    Task task = tasks.computeIfAbsent(generator, g -> {
      Task newTask = new Task(generator);
      tasks.put(generator, newTask);
      unstartedTasks.addLast(newTask);
      return newTask;
    });

    generatorDependencies.forEach(s -> {
      Task d = tasks.get(s);
      Preconditions.checkNotNull(d, "Generator dependency that was never created?");
      task.dependencies.add(d);
    });

    //A generator asked for this so remember the dependency, in case we didn't have it.
    if (!thread.stack.isEmpty()) {
      thread.stack.getLast().dependencies.add(task);
    }
  }

  void resultNeeded(Worker thread, String generator) {

    if (thread.stack.isEmpty()) {
      Preconditions.checkState(thread.topWaitingForGenerator == null);
      Preconditions.checkState(thread.topWaitingForData == null);
      thread.topWaitingForGenerator = generator;
    } else {
      Task task = thread.stack.getLast();
      Preconditions.checkState(task.blockingGenerator == null);
      Preconditions.checkState(task.blockingData == null);
      task.blockingGenerator = generator;
    }

    schedule();

  }

  void dataNeeded(Worker thread, String data) {

    if (thread.stack.isEmpty()) {
      Preconditions.checkState(thread.topWaitingForGenerator == null);
      Preconditions.checkState(thread.topWaitingForData == null);
      thread.topWaitingForData = data;
    } else {
      Task task = thread.stack.getLast();
      Preconditions.checkState(task.blockingGenerator == null);
      Preconditions.checkState(task.blockingData == null);
      task.blockingData = data;
    }

    schedule();

  }

  void executionComplete(Worker thread, String generator, Result result) {
    Task task = thread.stack.removeLast();
    Preconditions.checkState(task.generator.equals(generator));
    task.result = result;
    schedule();
  }

  public Planner.MachineToPlanner connectMachine(PlannerToMachine machine, InetSocketAddress address) {
    Machine plannedMachine = new Machine(this, machine, address);
    commands.add(() -> {
      machines.add(plannedMachine);
    });

    return new MachineForwarder(this, plannedMachine);
  }

  void fetchComplete(Machine machine, String digest, InetSocketAddress address, boolean success) {
    Data d = data.get(digest);
    machine.pendingData = null;
    if (success) {
      d.machines.add(machine);

    } else {
      d.machines.removeIf(test -> test.address.equals(address));
    }

    schedule();
  }

  void dataAvailable(Worker plannedThread, String digest) {
    Data d = data.computeIfAbsent(digest, s -> new Data());

    d.machines.add(plannedThread.machine);
  }

  public void machineDied(Machine machine) {
    for (Worker w : machine.threads) {
      for (Task t : w.stack) {
        t.blockingGenerator = null;
        t.blockingData = null;
        t.hasStarted = false;
      }

    }

    machines.remove(machine);

    schedule();
  }

  public void flush(Machine machine) {
    machine.remote.flushed();
  }

  void addThreadToMachine(Worker ret, Machine plannedMachine) {
    plannedMachine.threads.add(ret);
  }

  void queue(Runnable r) {
    commands.add(r);
  }

  private static class Data {

    Set<Machine> machines = Sets.newHashSet();


  }

  private static class Task {

    final String generator;
    boolean hasStarted;
    Result result;

    String blockingData;
    String blockingGenerator;

    Set<Task> dependencies = Sets.newHashSet();


    public Task(String generator) {
      this.generator = generator;
    }
  }

  static class Machine {

    final InetSocketAddress address;
    private final PlannerToMachine remote;
    Set<Worker> threads = Sets.newHashSet();
    String pendingData;
    private Set<String> knownGenerators = Sets.newHashSet();

    Machine(PlannerImpl planner, PlannerToMachine store, InetSocketAddress address) {
      this.remote = store;
      this.address = address;

    }

  }

  static class Worker {

    private final PlannerToThread remote;
    private final Machine machine;
    boolean isWorker = false;
    String waitingForData = null;

    LinkedList<Task> stack = Lists.newLinkedList();

    String topWaitingForGenerator;
    String topWaitingForData;

    public Worker(PlannerImpl planner, Machine machine, PlannerToThread worker) {

      this.machine = machine;
      this.remote = worker;
    }

  }

}
