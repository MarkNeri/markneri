package markneri.launcher;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipException;

/**
 * Created by markneri on 3/17/2017.
 */
public class JarBuilder {


    private static boolean addFile(JarOutputStream dst, String name, File file) throws IOException {




        try {

            final JarEntry jarEntry = new JarEntry(name);
            jarEntry.setTime(0);
            jarEntry.setLastAccessTime(FileTime.fromMillis(0));
            jarEntry.setLastModifiedTime(FileTime.fromMillis(0));

            dst.putNextEntry(jarEntry);

            FileUtils.copyFile(file, dst);
        } catch (ZipException e) {
            throw Throwables.propagate(e);
        } finally {


            dst.closeEntry();
        }
        return true;
    }

    private static boolean addDirectory(JarOutputStream jos, String name, File dir, boolean isRoot) throws IOException {
        boolean hasAnyFiles = false;


        File[] contents = dir.listFiles();
        if (contents != null) {

            if (!isRoot) {
                String dirName = name.endsWith("/") ? name : (name + "/");

                jos.putNextEntry(new JarEntry(name));
                jos.closeEntry();
            }

            for (File f : contents) {
                String itemName = (name.length() == 0 ? f.getName() : name + "/" + f.getName());

                if (f.isDirectory()) {
                    hasAnyFiles |= addDirectory(jos, itemName, f, false);
                } else {
                    hasAnyFiles |= addFile(jos, itemName, f);
                }
            }
        }


        return hasAnyFiles;

    }


    public static boolean create(File pathToJar, File fsource) throws IOException {

        boolean hasAnyFiles = false;

        FileOutputStream out = new FileOutputStream(pathToJar);
        JarOutputStream jarOutputStream = new JarOutputStream(out);


        if (fsource.isDirectory()) {
            hasAnyFiles |= addDirectory(jarOutputStream, "", fsource, true);
        } else if (fsource.isFile()) {
            hasAnyFiles = true;
            addFile(jarOutputStream, fsource.getName(), fsource);
        }


        try {
            jarOutputStream.close();
            IOUtils.closeQuietly(out);



        } catch (ZipException e) {
            if (hasAnyFiles) throw new RuntimeException(e);
        }
        return hasAnyFiles;
    }


}
