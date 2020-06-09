package com.flexicore.runningentities;

import com.flexicore.constants.Constants;
import com.flexicore.interfaces.FlexiCoreClassLoader;
import com.flexicore.utils.InheritanceUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Created by Asaf on 20/04/2017.
 */

//TODO: redirect classes that were not loaded to plugins
public class RemovableURLClassLoader extends URLClassLoader implements FlexiCoreClassLoader {
    private Logger logger;
    private Object urlsStack = null;
    private ArrayList<URL> path = null;
    private HashMap lmap = null;
    private ArrayList loaders = null;
    private List<Pair<JarFile, URL>> availablePlugins = new ArrayList<>();

    public RemovableURLClassLoader(Logger logger, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.logger = logger;
        enableRemoveUrl();
    }

    public RemovableURLClassLoader(Logger logger, URL[] urls) {
        super(urls);
        this.logger = logger;
        enableRemoveUrl();


    }

    public RemovableURLClassLoader(Logger logger, URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
        this.logger = logger;
        enableRemoveUrl();


    }

    Class<?> defineClassForProxyService(String name, byte[] b, int off, int len,
                                        ProtectionDomain protectionDomain) {
        return defineClass(name, b, off, len, protectionDomain);
    }

    Class<?> defineClassForProxyService(String name, byte[] b, int off, int len) {
        return defineClass(name, b, off, len);
    }


    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            logger.fine("loading " + name);
            try {
                Class<?> c = super.loadClass(name);
                try {
                        InheritanceUtils.registerClass(c, false);
                } catch (Throwable e) {
                    logger.log(Level.WARNING, "unable to register class", e);
                }
                logger.fine("Shared Class Loader loaded: " + c.getCanonicalName());
                return c;
            } catch (ClassNotFoundException e) {
                for (Pair<JarFile, URL> availablePlugin : availablePlugins) {
                    try {
                        addUrlToParent(availablePlugin.getRight());
                        Class<?> c = findClass(name);


                        removeUrlFromParent(availablePlugin.getLeft(), availablePlugin.getRight());
                        logger.fine("Shared Class Loader loaded Dynamically: " + c.getCanonicalName());
                        try {
                            InheritanceUtils.registerClass(c, false);
                        } catch (Throwable e1) {
                            logger.log(Level.WARNING, "unable to register class", e1);
                        }

                        return c;

                    } catch (Exception ignored) {


                    } finally {
                        removeUrlFromParent(availablePlugin.getLeft(), availablePlugin.getRight());
                    }
                }
                throw e;

            }
        }


    }


    private void enableRemoveUrl() {
        if (Constants.JDK8) {
            enableRemoveUrlJDK8();
        } else {
            enableRemoveUrlJDK11();
        }
    }

    private void enableRemoveUrlJDK11() {

        Class<URLClassLoader> clazz = URLClassLoader.class;
        Field f;
        try {
            f = clazz.getDeclaredField("ucp");
            f.setAccessible(true);
            Object ucp = f.get(this);
            Class<?> URLClassPathClass = ucp.getClass();
            f = URLClassPathClass.getDeclaredField("unopenedUrls");
            f.setAccessible(true);
            urlsStack = f.get(ucp);
            f = URLClassPathClass.getDeclaredField("path");
            f.setAccessible(true);
            path = (ArrayList<URL>) f.get(ucp);
            f = URLClassPathClass.getDeclaredField("lmap");
            f.setAccessible(true);
            lmap = (HashMap) f.get(ucp);
            f = URLClassPathClass.getDeclaredField("loaders");
            f.setAccessible(true);
            loaders = (ArrayList) f.get(ucp);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    private void enableRemoveUrlJDK8() {

        Class<URLClassLoader> clazz = URLClassLoader.class;
        Field f;
        try {
            f = clazz.getDeclaredField("ucp");
            f.setAccessible(true);
            Object ucp = f.get(this);
            Class<?> URLClassPathClass = ucp.getClass();
            f = URLClassPathClass.getDeclaredField("urls");
            f.setAccessible(true);
            urlsStack = f.get(ucp);
            f = URLClassPathClass.getDeclaredField("path");
            f.setAccessible(true);
            path = (ArrayList<URL>) f.get(ucp);
            f = URLClassPathClass.getDeclaredField("lmap");
            f.setAccessible(true);
            lmap = (HashMap) f.get(ucp);
            f = URLClassPathClass.getDeclaredField("loaders");
            f.setAccessible(true);
            loaders = (ArrayList) f.get(ucp);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void removeUrlFromParent(JarFile jar, URL url) {
        path.remove(url);
        List<Object> toRemove2 = new ArrayList<>();
        Set<Map.Entry> set = lmap.entrySet();
        for (Map.Entry entry : set) {
            Object object = entry.getKey();
            if (object instanceof String) {
                String s = (String) object;
                if (s.contains(jar.getName().replace("\\", "/"))) {
                    toRemove2.add(s);
                }
            }
        }
        for (Object o : toRemove2) {
            lmap.remove(o);
        }
        if (!loaders.isEmpty()) {
            loaders.remove(0);
        }
    }

    public void removeAllUrls() {
        if (Constants.JDK8) {
            removeAllUrlsJDK8();
        } else {
            removeAllUrlsJDK11();
        }
    }

    private void removeAllUrlsJDK11() {
        ((ArrayDeque) urlsStack).clear();
    }


    private void removeAllUrlsJDK8() {
        ((Stack) urlsStack).clear();
    }

    public void addUrlToParent(URL url) {
        if (Constants.JDK8) {
            addUrlToParentJDK8(url);
        } else {
            addUrlToParentJDK11(url);
        }
    }

    private void addUrlToParentJDK8(URL url) {
        if (!path.contains(url)) {
            ((Stack) urlsStack).add(0, url);
            path.add(url);
        }
    }


    private void addUrlToParentJDK11(URL url) {
        if (!path.contains(url)) {
            ((ArrayDeque) urlsStack).addFirst(url);
            path.add(url);
        }
    }


    public void registerPlugin(Pair<JarFile, URL> plugin) {
        availablePlugins.add(plugin);
    }

    @Override
    public String toString() {
        return "RemovableURLClassLoader{} " + super.toString();
    }
}
