package comp.impl.planner;

import com.google.common.collect.Maps;
import common.Exceptions;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


public class PlannerClient implements Planner {

  final InetSocketAddress address;


  public PlannerClient(InetSocketAddress address) {
    this.address = address;
  }

  @Override
  public MachineToPlanner connectMachine(PlannerToMachine machine, InetSocketAddress dataAddress) {
    ClientSidePlannerConnection ret = new ClientSidePlannerConnection(address, machine, dataAddress);
    ret.start();
    return ret;
  }

  static class ClientSidePlannerConnection extends Thread implements MachineToPlanner {

    final ObjectOutputStream toServer;
    final ObjectInputStream fromServer;
    final Socket socket;
    final PlannerToMachine plannerToMachine;
    final AtomicInteger freeThreadId = new AtomicInteger();
    final Map<Integer, PlannerToThread> threads = Maps.newHashMap();


    ClientSidePlannerConnection(InetSocketAddress address, PlannerToMachine plannerToMachine, InetSocketAddress dataAddress) {
      try {
        socket = new Socket(address.getAddress(), address.getPort());
        toServer = new ObjectOutputStream(socket.getOutputStream());
        fromServer = new ObjectInputStream(socket.getInputStream());

        toServer.writeObject(dataAddress);

      } catch (Exception e) {
        throw Exceptions.toRuntime(e);
      }

      this.plannerToMachine = plannerToMachine;
    }

    @Override
    public ThreadToPlanner connectThread(PlannerToThread worker) {
      final int threadI = freeThreadId.getAndIncrement();
      threads.put(threadI, worker);

      send((cc) -> {

        PlannerServer.ServerSideThreadConnection plannerToThread = new PlannerServer.ServerSideThreadConnection(
            cc, threadI);
        cc.threads.put(threadI, cc.toPlanner.connectThread(plannerToThread));
      });

      return new ClientSideThreadConnection(this, threadI);

    }

    @Override
    public void fetchComplete(String digest, InetSocketAddress address, boolean success) {
      send((cc) -> {
        cc.toPlanner.fetchComplete(digest, address, success);
      });
    }

    @Override
    public void machineDead() {
      send((cc) -> {
        cc.toPlanner.machineDead();
      });
    }

    @Override
    public void flush() {
      send((cc) -> {
        cc.toPlanner.flush();
      });
    }


    public void run() {
      try {
        while (true) {
          PlannerServer.ToClient c = ((PlannerServer.ToClient) fromServer.readObject());
          c.apply(this);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    private <T extends PlannerServer.ToServer & Serializable> void send(T toSend) {
      try {
        synchronized (toServer) {
          toServer.writeObject(toSend);
        }
        toServer.flush();
      } catch (IOException e) {
        throw Exceptions.toRuntime(e);
      }

    }
  }

  private static class ClientSideThreadConnection implements ThreadToPlanner {

    private final ClientSidePlannerConnection connection;
    private final int threadI;

    public ClientSideThreadConnection(ClientSidePlannerConnection clientSidePlannerConnection,
        int threadI) {
      this.connection = clientSidePlannerConnection;
      this.threadI = threadI;
    }

    @Override
    public void scheduleGenerator(String generator, Iterable<String> generatorDependencies) {
      final int threadI = this.threadI;
      connection.send((cc) ->
      {
        cc.threads.get(threadI).scheduleGenerator(generator, generatorDependencies);
      });
    }

    @Override
    public void threadIdle() {
      final int threadI = this.threadI;
      connection.send((cc) ->
      {
        cc.threads.get(threadI).threadIdle();
      });

    }

    @Override
    public void resultNeeded(String generatorDigest) {
      final int threadI = this.threadI;
      connection.send((cc) ->
      {
        cc.threads.get(threadI).resultNeeded(generatorDigest);
      });

    }

    @Override
    public void dataNeeded(String dataDigest) {
      final int threadI = this.threadI;
      connection.send((cc) ->
      {
        cc.threads.get(threadI).dataNeeded(dataDigest);
      });
    }

    @Override
    public void executionComplete(String generator, Result result) {
      final int threadI = this.threadI;
      connection.send((cc) ->
      {
        cc.threads.get(threadI).executionComplete(generator, result);
      });

    }

    @Override
    public void dataAvailable(String digest) {
      final int threadI = this.threadI;
      connection.send((cc) ->
      {
        cc.threads.get(threadI).dataAvailable(digest);
      });
    }
  }


}
