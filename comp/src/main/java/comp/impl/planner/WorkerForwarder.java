package comp.impl.planner;

/*
Forwards messages from a Worker to a PlannerImpl

Synchronizes and attaches the PlannerImpl's Worker to each message
*/
public class WorkerForwarder implements Planner.ThreadToPlanner {

  protected final PlannerImpl planner;
  final PlannerImpl.Worker thread;

  public WorkerForwarder(PlannerImpl planner, PlannerImpl.Worker thread) {
    this.planner = planner;
    this.thread = thread;
  }

  public void scheduleGenerator(String generator, Iterable<String> generatorDependencies) {
    planner.queue(() -> planner.scheduleGenerator(thread, generator, generatorDependencies));
  }

  public void threadIdle() {
    planner.queue(() -> planner.idleThread(thread));
  }

  public void resultNeeded(String generatorDigest) {
    planner.queue(() -> planner.resultNeeded(thread, generatorDigest));
  }

  public void dataNeeded(String dataDigest) {
    planner.queue(() -> planner.dataNeeded(thread, dataDigest));

  }

  public void executionComplete(String generatorDigest, Planner.Result dataDigest) {
    planner.queue(() -> planner.executionComplete(thread, generatorDigest, dataDigest));
  }

  public void dataAvailable(String digest) {
    planner.queue(() -> planner.dataAvailable(thread, digest));
  }
}
