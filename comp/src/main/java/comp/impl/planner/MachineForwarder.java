package comp.impl.planner;

import java.net.InetSocketAddress;

/*
Forwards messages from a Machine to a PlannerImpl

Sychronizes and attaches the PlannerImpl's Machine to the message
*/
public class MachineForwarder implements Planner.MachineToPlanner {

  protected final PlannerImpl planner;
  private final PlannerImpl.Machine machine;

  public MachineForwarder(PlannerImpl planner, PlannerImpl.Machine machine) {
    this.planner = planner;
    this.machine = machine;
  }

  @Override
  public Planner.ThreadToPlanner connectThread(Planner.PlannerToThread worker) {

    PlannerImpl.Worker ret = new PlannerImpl.Worker(planner, machine, worker);

    planner.queue(() -> planner.addThreadToMachine(ret, machine));

    return new WorkerForwarder(planner, ret);
  }

  @Override
  public void fetchComplete(String digest, InetSocketAddress address, boolean success) {
    planner.queue(() -> planner.fetchComplete(machine, digest, address, success));
  }

  @Override
  public void machineDead() {
    planner.queue(() -> planner.machineDied(machine));
  }

  @Override
  public void flush() {
    planner.queue(() -> planner.flush(machine));
  }
}
