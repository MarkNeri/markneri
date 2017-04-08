package common;

import com.google.common.base.Preconditions;
import com.google.common.collect.Queues;
import java.util.concurrent.BlockingQueue;


public interface CommandQueue {

  static Steppable createSteppable() {
    return new Impl();
  }

  static CommandQueue createDaemon(String name) {
    final Impl ret = new Impl();
    Thread t = new Thread() {
      public void run() {
        try {
          while (true) {
            ret.step();
          }
        } catch (Exception e) {
          e.printStackTrace();
        }

      }
    };
    t.setDaemon(true);
    t.setName(name);
    t.start();
    return ret;
  }

  void add(Runnable r);

  void flush();

  boolean getIsFlushing();

  interface Steppable extends CommandQueue {

    void step();
  }

  static class Impl implements Steppable {

    private final BlockingQueue<Runnable> commandQueue = Queues.newLinkedBlockingQueue();
    boolean isFlushing = false;

    public void step() {
      try {
        commandQueue.take().run();
      } catch (InterruptedException e) {
        throw Exceptions.toRuntime(e);
      }
    }

    public boolean getIsFlushing() {
      return isFlushing;
    }

    public void flush() {
      Preconditions.checkState(!getIsFlushing());

      isFlushing = true;
      while (!commandQueue.isEmpty()) {
        step();
      }
      isFlushing = false;
    }

    @Override
    public void add(Runnable r) {
      commandQueue.add(r);
    }
  }

}
