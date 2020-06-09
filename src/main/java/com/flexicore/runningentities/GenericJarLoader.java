/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flexicore.runningentities;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.flexicore.annotations.plugins.PluginInfo;
import com.flexicore.constants.Constants;

import com.flexicore.interfaces.FlexiCoreClassLoader;
import com.flexicore.model.ModuleManifest;
import com.flexicore.utils.InheritanceUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;


/**
 * Extends URLClassloader
 *
 * @author Asaf
 */
public class GenericJarLoader extends URLClassLoader implements FlexiCoreClassLoader {
    private Path classFolder;
    private Logger logger;
    private RemovableURLClassLoader parent;
    private URL[] urls;


    @SuppressWarnings({"restriction"})
    public GenericJarLoader(URL[] urls, Path classFolder, RemovableURLClassLoader classLoader) {

        super(urls, classLoader);
        this.urls = urls;
        this.classFolder = classFolder;
        this.parent = classLoader;

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
        synchronized (getClassLoadingLock(name)){
            logger.fine(classFolder+": loading "+name);
            Class<?> aClass = super.loadClass(name);
            if (aClass != null) {
                try {
                    InheritanceUtils.registerClass(aClass, false);

                } catch (Throwable e) {
                    logger.log(Level.WARNING, "Unable to register class", e);
                }
            }
            return aClass;
        }

    }


    /**
     * loads a jar
     *
     * @param pathToJar
     * @param moduleManifest
     * @return list of loaded class
     */
    public ArrayList<Class<?>> loadJar(String pathToJar, ModuleManifest moduleManifest) {
        JarFile jar;
        try {
            jar = new JarFile(classFolder + Constants.SEPERATOR + pathToJar);
            List<JarEntry> e = jar.stream().sorted(Comparator.comparing(f -> f.getName())).collect(Collectors.toList());
            Class<?> c;
            String className;
            ArrayList<Class<?>> loaded = new ArrayList<>();
            Set<String> toLoad = new ConcurrentSkipListSet<>();
            Set<String> interfaces = new ConcurrentSkipListSet<>();
            if (moduleManifest != null) {
                interfaces.addAll(moduleManifest.getProvides());

            }
            parent.registerPlugin(Pair.of(jar, urls[0]));
            for (JarEntry je : e) {

                if (je.isDirectory() || (!je.getName().endsWith(".class"))) {
                    continue;
                }

                className = je.getName().substring(0, je.getName().length() - 6);
                className = className.replace('/', '.');
                if (isDependentClass(className, interfaces) || isPluginClass(jar.getInputStream(je))) {
                    toLoad.add(className);
                }


            }

            HashSet<String> toRemove = new HashSet<>();
            for (String classNameToLoad : toLoad) {
                for (String string : interfaces) {
                    if (classNameToLoad.contains(string)) {
                        try {
                            parent.addUrlToParent(urls[0]);
                            c = parent.loadClass(classNameToLoad);
                            logger.fine("provided class discovered " + c.getCanonicalName());
                            Method[] methods = c.getMethods();
                            for (Method method : methods) {
                                logger.fine("found method " + method.toGenericString());
                            }
                            loaded.add(c);
                            toRemove.add(classNameToLoad);
                            parent.removeUrlFromParent(jar, urls[0]);

                        } catch (ClassNotFoundException | IllegalArgumentException e1) {

                            logger.warning("unable to load provided class : 1 " + classNameToLoad);
                        }

                    }
                }
            }
            parent.removeAllUrls();
            toLoad.removeAll(toRemove);
            for (String classNameToLoad : toLoad) {


                try {
                    logger.fine( "trying to load class: " + classNameToLoad);
                    c = loadClass(classNameToLoad); // loads the class on this
                    // specific class loaded
                    // , will load dependent classes if not loaded before here
                    // or by any parent Classloader

                    loaded.add(c);
                    logger.fine( "loaded class: " + classNameToLoad);
                } catch (Exception e1) {
                    logger.log(Level.WARNING, "loading class " + classNameToLoad + " failed,", e1);

                }
            }
            jar.close();
            return loaded;
        } catch (IOException e) {
            logger.log(Level.WARNING, "file is not a valid jar", e);

            return null;

        }
    }


    private boolean isDependentClass(String className, Set<String> interfaces) {
        for (String anInterface : interfaces) {
            if (className.contains(anInterface)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPluginClass(InputStream inputStream) {
        try {

            String s = IOUtils.toString(inputStream);
            boolean ans = s.contains(PluginInfo.class.getCanonicalName().replace(".", "/"));
            inputStream.close();
            return ans;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "unable to parse class file", e);
        }
        return false;


    }


    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public String toString() {
        return "GenericJarLoader{" +
                "urls=" + Arrays.toString(urls) +
                "} " + super.toString();
    }
}
