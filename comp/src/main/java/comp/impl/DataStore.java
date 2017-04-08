package comp.impl;

import common.Cache;
import common.Exceptions;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;


public class DataStore {

  final Cache<String, Serializable> objects = Cache.create();
  private final InetSocketAddress address;
  Cache<InetSocketAddress, OutgoingConnection> outgoingConnections = Cache.create();


  DataStore() {
    final ServerSocket s;
    try {
      s = new ServerSocket(0);
    } catch (IOException e) {
      throw Exceptions.toRuntime(e);
    }

    address = new InetSocketAddress("127.0.0.1", s.getLocalPort());

    new Thread(() -> {
      try {
        while (true) {
          Socket cs = s.accept();
          new IncomingConnection(cs).start();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }).start();
  }

  Serializable putData(String digest, Serializable object) {
    return objects.putIfAbsent(digest, object);
  }

  InetSocketAddress getSocketAddress() {
    return address;
  }

  public Optional<Object> fetchData(String digest, InetSocketAddress a) {

    OutgoingConnection o = outgoingConnections.get(a, OutgoingConnection::new);
    Optional<Serializable> opt = o.get(digest);
    if (opt.isPresent()) {
      return Optional.of(objects.putIfAbsent(digest, opt.get()));
    } else {
      return Optional.empty();
    }
  }

  Optional<Serializable> getObject(String digest) {
    return objects.getOptional(digest);
  }

  private class IncomingConnection extends Thread {

    private final ObjectOutputStream toClient;
    private final ObjectInputStream fromClient;
    private final Socket socket;

    public IncomingConnection(Socket socket) throws IOException {
      this.socket = socket;
      toClient = new ObjectOutputStream(socket.getOutputStream());
      fromClient = new ObjectInputStream(socket.getInputStream());
    }

    public void run() {
      try {

        while (true) {

          String digest = fromClient.readUTF();
          toClient.writeObject(objects.ifPresent(digest, (v -> v), () -> null));
        }

      } catch (Exception e) {
        Exceptions.toRuntime(e);
      }
    }
  }

  class OutgoingConnection {

    private final ObjectOutputStream toServer;
    private final ObjectInputStream fromServer;

    OutgoingConnection(InetSocketAddress address) {
      Socket s = new Socket();
      try {
        s.connect(address);
        toServer = new ObjectOutputStream(s.getOutputStream());
        fromServer = new ObjectInputStream(s.getInputStream());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    Optional<Serializable> get(String digest) {
      try {
        toServer.writeUTF(digest);
        toServer.flush();
        return Optional.ofNullable((Serializable) fromServer.readObject());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

  }
}
