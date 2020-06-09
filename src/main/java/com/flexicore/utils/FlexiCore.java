package com.flexicore.utils;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Asaf on 29/01/2017.
 */
public class FlexiCore {
    private static Map<String,File> loaded= new ConcurrentHashMap<>();




    public static void loadLibrary(String name,Class<?> c){
        try {
            ClassLoader cl=c.getClassLoader();

            Method loadLibrary0=Runtime.class.getDeclaredMethod("load0", Class.class, String.class);
            loadLibrary0.setAccessible(true);

            String fullPath= getLibraryPath(name);
            //File dir=Files.createTempDir();
            File orig=new File(fullPath);
           // dir.deleteOnExit();
            String newName=orig.getName()+System.identityHashCode(cl);
            File file=loaded.get(newName);
            if(file!=null){
                return;
            }
            File dir=Files.createTempDir();
            file=new File(dir,newName);
            dir.deleteOnExit();
            Files.copy(orig,file);
          //  addDir(dir.getAbsolutePath());
            //addDir(file.getAbsolutePath());
            loadLibrary0.invoke(Runtime.getRuntime(),c,file.getAbsolutePath());
            loaded.put(newName,file);
        } catch (NoSuchMethodException | IllegalAccessException | IOException | InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    public static void addDir(String s) throws IOException {
        try {
            // This enables the java.library.path to be modified at runtime
            // From a Sun engineer at http://forums.sun.com/thread.jspa?threadID=707176
            //
            Field field = ClassLoader.class.getDeclaredField("usr_paths");
            field.setAccessible(true);
            String[] paths = (String[]) field.get(null);
            for (String path : paths) {
                if (s.equals(path)) {
                    return;
                }
            }
            String[] tmp = new String[paths.length + 1];
            System.arraycopy(paths, 0, tmp, 0, paths.length);
            tmp[paths.length] = s;
            field.set(null, tmp);
            System.setProperty("java.library.path", System.getProperty("java.library.path") + File.pathSeparator + s);
        } catch (IllegalAccessException e) {
            throw new IOException("Failed to get permissions to set library path");
        } catch (NoSuchFieldException e) {
            throw new IOException("Failed to get field handle to set library path");
        }
    }

    public static String getLibraryPath(String name) throws IOException {
        try {
            // This enables the java.library.path to be modified at runtime
            // From a Sun engineer at http://forums.sun.com/thread.jspa?threadID=707176
            //
            Field userPathsField = ClassLoader.class.getDeclaredField("usr_paths");
            userPathsField.setAccessible(true);
            String[] userPaths = (String[]) userPathsField.get(null);


            Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
            sysPathsField.setAccessible(true);
            String[] sysPaths = (String[]) sysPathsField.get(null);

            String file11 = getLibAbsPath(name, sysPaths);
            if (file11 != null) return file11;
            String file1 = getLibAbsPath(name, userPaths);
            if (file1 != null) return file1;


        } catch (IllegalAccessException e) {
            throw new IOException("Failed to get permissions to set library path");
        } catch (NoSuchFieldException e) {
            throw new IOException("Failed to get field handle to set library path");
        }
        throw new IOException("Could not get Library Path");
    }

    private static String getLibAbsPath(String name, String[] possiblePaths) {
        for (String sysPath : possiblePaths) {
           File file=new File(sysPath);
           if(file.exists()){
               for (File file1 : file.listFiles()) {
                   if(file1.getName().contains(name)){
                       return file1.getAbsolutePath();
                   }
               }
           }


        }
        return null;
    }
}
