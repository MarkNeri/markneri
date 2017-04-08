package markneri.bootstrap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;


public class Bootstrap {

  public static final java.lang.String PORT = "Bootstrap Port ";
  static private boolean isInBootstrapMain = false;

  static public boolean isInBootstrapMain() {
    return isInBootstrapMain;
  }

  public static void main(String[] args)
      throws IOException, ClassNotFoundException, InterruptedException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
    isInBootstrapMain = true;

    ServerSocket serverSocket = new ServerSocket(0);

    //Send the sockert address over stdout, where Launcher expects it.
    System.out.println(PORT + serverSocket.getLocalPort());

    Socket clientSocket = serverSocket.accept();

    DataOutputStream toLauncher = new DataOutputStream(clientSocket.getOutputStream());
    DataInputStream fromLauncher = new DataInputStream(clientSocket.getInputStream());

    int numJars = fromLauncher.readInt();

    URL[] urls = new URL[numJars];
    String[] namesToDownload = new String[numJars];

    //Read the required jars, check if we have them and report
    byte[] buffer = new byte[65536];
    for (int i = 0; i < numJars; i++) {
      String name = fromLauncher.readUTF();

      File file = new File(name);
      boolean exists = file.exists();
      toLauncher.writeBoolean(exists);
      if (exists) {
        urls[i] = file.toURI().toURL();
      } else {
        namesToDownload[i] = name;
      }
    }
    toLauncher.flush();

    //Receive any missing jars
    for (int i = 0; i < numJars; i++) {
      String name = namesToDownload[i];
      if (name != null) {
        int length = fromLauncher.readInt();

        File file = new File(name);

        urls[i] = file.toURI().toURL();
        FileOutputStream stream = new FileOutputStream(file);
        int o = 0;
        while (o < length) {
          int numRead = fromLauncher.read(buffer, 0, Math.min(buffer.length, length - o));

          if (numRead <= 0) {
            throw new RuntimeException();
          }

          stream.write(buffer, 0, numRead);

          o += numRead;
        }
        stream.flush();
        stream.close();
      }
    }

    //Launch the requested class
    String clsName = fromLauncher.readUTF();
    URLClassLoader urlClassLoader = new URLClassLoader(urls);
    try {
      urlClassLoader.loadClass(clsName).getMethod("main", String[].class).invoke(null, new Object[]{args});
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException) {
        throw (RuntimeException) (e.getCause());
      }
      throw e;
    }

  }
}

