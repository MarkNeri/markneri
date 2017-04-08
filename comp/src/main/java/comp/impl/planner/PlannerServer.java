package comp.impl.planner;

import com.google.common.collect.Maps;
import common.Exceptions;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import org.apache.commons.io.IOUtils;


public class PlannerServer {

  final Planner delegate;
  private final int port;

  public PlannerServer(Planner delegate) {
    this.delegate = delegate;
    final ServerSocket s;
    try {
      s = new ServerSocket(0);
    } catch (IOException e) {
      throw Exceptions.toRuntime(e);
    }
    port = s.getLocalPort();

    new Thread(() -> {
      try {
        while (true) {
          Socket cs = s.accept();
          new ServerSidePlannerConnection(delegate, cs).start();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }


    }).start();
  }

  public int getPort() {
    return port;
  }


  interface ToServer extends Serializable {

    void apply(ServerSidePlannerConnection client);
  }

  interface ToClient extends Serializable {

    void apply(PlannerClient.ClientSidePlannerConnection thread);
  }

  static class ServerSidePlannerConnection extends Thread implements Planner.PlannerToMachine {

    final Socket socket;
    final Planner.MachineToPlanner toPlanner;
    final ObjectOutputStream toClient;
    final ObjectInputStream fromClient;
    final Map<Integer, Planner.ThreadToPlanner> threads = Maps.newHashMap();

    public ServerSidePlannerConnection(Planner delegate, Socket socket)
        throws IOException, ClassNotFoundException {
      this.socket = socket;
      toClient = new ObjectOutputStream(socket.getOutputStream());
      fromClient = new ObjectInputStream(socket.getInputStream());

      InetSocketAddress address = (InetSocketAddress) fromClient.readObject();

      toPlanner = delegate.connectMachine(this, address);
    }

    public void run() {
      while (true) {
        final ToServer c;
        try {
          c = (ToServer) fromClient.readObject();

        } catch (Exception e) {
          toPlanner.machineDead();
          IOUtils.closeQuietly(socket);
          IOUtils.closeQuietly(toClient);
          IOUtils.closeQuietly(fromClient);
          break;
        }

        c.apply(this);
      }
    }

    @Override
    public void result(String generator, Planner.Result result) {
      send((cc) -> {
        cc.plannerToMachine.result(generator, result);
      });
    }

    @Override
    public void fetchData(String digest, InetSocketAddress address) {
      send((cc) -> {
        cc.plannerToMachine.fetchData(digest, address);
      });
    }

    @Override
    public void flushed() {
      send((cc) -> {
        cc.plannerToMachine.flushed();
      });
    }

    private <T extends Serializable & ToClient> void send(ToClient c) {
      try {
        toClient.writeObject(c);
      } catch (IOException e) {
        throw Exceptions.toRuntime(e);
      }
    }
  }

  static class ServerSideThreadConnection implements Planner.PlannerToThread {

    private final ServerSidePlannerConnection connection;
    private final int index;


    ServerSideThreadConnection(ServerSidePlannerConnection connection, int index) {
      this.connection = connection;
      this.index = index;
    }

    @Override
    public void execute(String generatorDigest) {
      final int index = this.index;
      connection.send((cc) ->
      {
        cc.threads.get(index).execute(generatorDigest);
      });
    }

    @Override
    public void proceed() {
      final int index = this.index;
      connection.send((cc) ->
      {
        cc.threads.get(index).proceed();
      });

    }

    @Override
    public void abort() {
      final int index = this.index;
      connection.send((cc) ->
      {
        cc.threads.get(index).abort();
      });

    }
  }


}
