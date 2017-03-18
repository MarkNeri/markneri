package markneri.launcher;

import markneri.bootstrap.Bootstrap;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.jcraft.jsch.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by markneri on 3/17/2017.
 */
public class RemoteExecute {

    public RemoteExecute jvmWithArgs(String jvm) {
        this.jvmWithArgs = jvm;
        return this;
    }

    String jvmWithArgs = "java";

    static class Upload {
        public Upload(String hash, File file) {
            this.hash = hash;
            this.file = file;
        }

        final String hash;
        final File file;
    }

    static <T> T get(Future<T> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private String privateKeyFile = System.getProperty("user.home") + File.separator + ".ssh" + File.separator + "id_rsa";

    public RemoteExecute privateKeyFile(String file) {
        this.privateKeyFile = privateKeyFile;
        return this;
    }

    private String username = System.getProperty("user.name");

    public RemoteExecute username(String username) {
        this.username = username;
        return this;
    }


    public void remoteExecute(String[] args, String host) {
        if (Bootstrap.isInBootstrapMain()) return;

        try {
            main(args, host);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        System.exit(0);

    }

    private int debugPort = 0;

    public RemoteExecute debugPort(int port) {
        this.debugPort = port;
        return this;
    }

    String bootstrapCache = "";

    public void main(String[] args, String host) throws IOException, JSchException, InterruptedException, ExecutionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        PlanUploads planUploads = new PlanUploads().invoke();
        List<Future<Upload>> bootstrapUploads = planUploads.getBootstrapUploads();
        List<Future<Upload>> uploads = planUploads.getUploads();

        Session session = createSession(host);

        scpUpload(session, bootstrapUploads);

        JavaChannel javaChannel = new JavaChannel(session, bootstrapUploads);

        new BootstrapChannel(session, javaChannel.port(), uploads);

        javaChannel.echoStdOut();

    }

    /**
     * Communicates with the Bootstrap class selectively transfer files and laucnh main();
     */
    class BootstrapChannel {
        private DataInputStream fromBootstrap;
        private DataOutputStream toBootstrap;

        BootstrapChannel(Session session, int port, List<Future<Upload>> uploads) throws IOException, ExecutionException, InterruptedException, JSchException {

            Channel channelToBootstrap = session.getStreamForwarder("127.0.0.1", port);

            channelToBootstrap.connect();
            fromBootstrap = new DataInputStream(channelToBootstrap.getInputStream());
            toBootstrap = new DataOutputStream(channelToBootstrap.getOutputStream());

            bootstrapUpload(uploads);

            callMain();
        }



        private void bootstrapUpload(List<Future<Upload>> uploads) throws IOException, InterruptedException, ExecutionException {
            toBootstrap.writeInt(uploads.size());

            for (Future<Upload> fu : uploads) {
                Upload u = fu.get();
                toBootstrap.writeUTF(u.hash);
            }

            toBootstrap.flush();

            for (Future<Upload> fu : uploads) {
                if (!fromBootstrap.readBoolean()) {
                    Upload u = fu.get();

                    toBootstrap.writeInt((int) u.file.length());
                    FileUtils.copyFile(u.file, toBootstrap);
                }
            }
        }

        private void callMain() throws IOException {
            toBootstrap.writeUTF(mainClass());
            toBootstrap.flush();
        }

        private String mainClass() {

            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (int i = stack.length - 1; i >= 0; i--) {
                String name = stack[i].getClassName();
                if (!name.startsWith("java.") &&
                        !name.startsWith("sun.") &&
                        !name.startsWith("com.intellij.")) {
                    return stack[i].getClassName();
                }
            }
            throw new RuntimeException("Can't find main class");
        }
    }

    /**
     * Launches a jvm, hooks up stdout and err, sets up a debug connections and finds the port for the Bootstrap class
     */
    class JavaChannel
    {
        private final BufferedReader fromJava;

        JavaChannel(Session session, List<Future<Upload>> bootstrapUploads) throws JSchException, IOException {
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(javaCommandLine(bootstrapUploads));
            channel.connect();

            copyStreamToPrintInThread(channel.getErrStream(), System.err);

            this.fromJava = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            this.port = handshakeWithJavaConsole(session, fromJava);
        }

        private  final int port;

        int port()
        {
            return port;
        }

        void echoStdOut()
        {
            copyStreamLines(fromJava, System.out);
        }

        private void copyStreamToPrintInThread(InputStream errStream, PrintStream err) {
            BufferedReader javaStdErr = new BufferedReader(new InputStreamReader(errStream));
            new Thread(() -> {copyStreamLines(javaStdErr, err);}).start();
        }

        private void copyStreamLines(BufferedReader javaStdErr, PrintStream err) {
            while (true) try {
                String s = javaStdErr.readLine();
                if (s == null) break;

                err.println(s);
            } catch (IOException e) {
            }
        }

        private int handshakeWithJavaConsole(Session session, BufferedReader fromJava) throws IOException, JSchException {
            String debugInfo = "Listening for transport dt_socket at address: ";

            while (true) {
                String line = fromJava.readLine();

                if (line.startsWith(debugInfo)) {
                    int debugPort = Integer.parseInt(line.substring(debugInfo.length()));

                    int localDebugPort = session.setPortForwardingL(RemoteExecute.this.debugPort, "127.0.0.1", debugPort);

                    System.out.println(debugInfo + localDebugPort);
                } else if (line.startsWith(Bootstrap.PORT)) {
                    return Integer.parseInt(line.substring(Bootstrap.PORT.length()));

                } else {
                    System.out.println(line);
                }
            }
        }

        private String javaCommandLine(List<Future<Upload>> bootstrapUploads) {
            //StringBuilder javaCommand = new StringBuilder("jdk1.8.0_121/jre/bin/java -cp ");
            StringBuilder javaCommand = new StringBuilder(jvmWithArgs + " -Xdebug -Xrunjdwp:server=y,transport=dt_socket,suspend=n -cp ");


            javaCommand.append(Joiner.on(File.pathSeparator).join(Iterables.transform(bootstrapUploads, b -> bootstrapCache + get(b).hash)));

            javaCommand.append(" ");
            javaCommand.append(Bootstrap.class.getName());
            return javaCommand.toString();
        }

    }










    private void scpUpload(Session session, List<Future<Upload>> bootstrapUploads) {
        for (Future<Upload> fu : bootstrapUploads) {
            Upload u = get(fu);
            scpFile(session, bootstrapCache + u.hash, u.file);
        }
    }


    private Session createSession(String host) throws JSchException {
        JSch jsch = new JSch();
        jsch.addIdentity(privateKeyFile);
        Session session = jsch.getSession(username, host, 22);
        jsch.setHostKeyRepository(new MyHostKeyRepository());
        session.connect();
        return session;
    }



    private static void scpFile(Session session, String targetName, File jarFile) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("scp -t .");

            inputStream = channel.getInputStream();
            outputStream = channel.getOutputStream();

            channel.connect();

            String command = "C0644 " + jarFile.length() + " " + targetName + "\n";

            outputStream.write(command.getBytes());
            inputStream.read();

            FileUtils.copyFile(jarFile, outputStream);

            outputStream.write(0);
            outputStream.flush();
        } catch (Exception e) {
            IOUtils.closeQuietly(outputStream);
            IOUtils.closeQuietly(inputStream);

            throw Throwables.propagate(e);
        }
    }

    private static class MyHostKeyRepository implements HostKeyRepository {
        @Override
        public int check(String host, byte[] key) {
            return HostKeyRepository.OK;
        }

        @Override
        public void add(HostKey hostkey, UserInfo ui) {

        }

        @Override
        public void remove(String host, String type) {

        }

        @Override
        public void remove(String host, String type, byte[] key) {

        }

        @Override
        public String getKnownHostsRepositoryID() {
            return null;
        }

        @Override
        public HostKey[] getHostKey() {
            return new HostKey[0];
        }

        @Override
        public HostKey[] getHostKey(String host, String type) {
            return new HostKey[0];
        }
    }

    private class PlanUploads {
        private List<Future<Upload>> bootstrapUploads;
        private List<Future<Upload>> uploads;

        public List<Future<Upload>> getBootstrapUploads() {
            return bootstrapUploads;
        }

        public List<Future<Upload>> getUploads() {
            return uploads;
        }

        public PlanUploads invoke() {
            ExecutorService executor = Executors.newCachedThreadPool();

            Predicate<String> isBootstrapCp = s -> s.contains("bootstrap");

            String classPaths = System.getProperty("java.class.path");

            List<File> jars = Lists.newArrayList();
            List<File> directories = Lists.newArrayList();

            List<File> bootstrapJars = Lists.newArrayList();
            List<File> bootstrapDirectories = Lists.newArrayList();

            final String jreString = File.separator + "jre" + File.separator + "lib" + File.separator;
            for (String classPath : classPaths.split(File.pathSeparator)) {
                if (classPath.contains(jreString)) {
                    //Skip it because it's part of the jre
                } else {
                    if (classPath.endsWith(".jar")) {
                        (isBootstrapCp.test(classPath) ? bootstrapJars : jars).add(new File(classPath));
                    } else {
                        (isBootstrapCp.test(classPath) ? bootstrapDirectories : directories).add(new File(classPath));
                    }
                }
            }


            bootstrapUploads = Lists.newArrayList();
            bootstrapUploads.addAll(uploadsForDirectories(executor, bootstrapDirectories, false));
            bootstrapUploads.addAll(uploadsForJars(executor, bootstrapJars));

            uploads = Lists.newArrayList();
            uploads.addAll(uploadsForDirectories(executor, directories, true));
            uploads.addAll(uploadsForJars(executor, jars));
            return this;
        }

        private String checksumFile(File file) throws IOException {
            try (FileInputStream inputStream = new FileInputStream(file)) {
                return DigestUtils.sha512Hex(inputStream);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }

        }

        private List<Future<Upload>> uploadsForDirectories(ExecutorService executor, List<File> directories, boolean renameToHash) {
            return directories.stream().map(
                    file -> executor.submit(() -> {
                        File tempBootstrapJar = File.createTempFile("classes", ".jar");
                        tempBootstrapJar.deleteOnExit();

                        new JarBuilder().create(
                                tempBootstrapJar,
                                file);

                        String hash = checksumFile(tempBootstrapJar) + ".jar";

                        final File finalFile;
                        if (renameToHash) {
                            File destFile = new File(tempBootstrapJar.getParent(), hash);
                            if (destFile.exists()) {
                                tempBootstrapJar.delete();
                            } else {
                                tempBootstrapJar.renameTo(destFile);
                            }

                            finalFile = destFile;
                        } else {
                            finalFile = tempBootstrapJar;
                        }
                        return new Upload(hash, finalFile);

                    })).collect(Collectors.toList());
        }

        private List<Future<Upload>> uploadsForJars(ExecutorService executor, List<File> jars) {
            return jars.stream().map(
                    jar -> executor.submit(() -> {
                        String hash = checksumFile(jar) + ".jar";
                        return new Upload(hash, jar);
                    })).collect(Collectors.toList());
        }
    }


}
