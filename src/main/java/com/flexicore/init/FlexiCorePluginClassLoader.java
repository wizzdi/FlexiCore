package com.flexicore.init;

import org.pf4j.PluginClassLoader;
import org.pf4j.PluginDependency;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FlexiCorePluginClassLoader extends PluginClassLoader {
    private String name;
    private boolean parentFirst;
    private PluginDescriptor pluginDescriptor;
    private PluginManager pluginManager;
    private static final Set<String> RESERVED = new HashSet<>(Arrays.asList("META-INF/extensions.idx", "META-INF/MANIFEST.MF"));
    private static final Logger log = LoggerFactory.getLogger(FlexiCorePluginClassLoader.class);
    private static final String JAVA_PACKAGE_PREFIX = "java.";
    private static final String PLUGIN_PACKAGE_PREFIX = "org.pf4j.";

    public FlexiCorePluginClassLoader(PluginManager pluginManager, PluginDescriptor pluginDescriptor, ClassLoader parent) {
        this(pluginManager, pluginDescriptor, parent, false);
    }

    private void initName(PluginDescriptor pluginDescriptor) {
        if (pluginDescriptor != null) {
            name = pluginDescriptor.getPluginId() + "@" + pluginDescriptor.getVersion();
        }
    }

    public FlexiCorePluginClassLoader(PluginManager pluginManager, PluginDescriptor pluginDescriptor, ClassLoader parent, boolean parentFirst) {
        super(pluginManager, pluginDescriptor, parent, parentFirst);
        initName(pluginDescriptor);
        this.parentFirst = parentFirst;
        this.pluginManager = pluginManager;
        this.pluginDescriptor = pluginDescriptor;


    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "FlexiCorePluginClassLoader{" + name + "}";
    }

    @Override
    public URL getResource(String name) {
        if (parentFirst && RESERVED.contains(name)) {
            log.trace("resource '{}' is in reserved list, will attempt to load it locally even though parent first is enabled", name);
            URL url = findResource(name);
            if (url != null) {
                log.trace("Found resource '{}' in plugin classpath", name);
                return url;
            }

            log.trace("Couldn't find resource '{}' in plugin classpath. Delegating to parent", name);
        }

        return super.getResource(name);
    }

    /**
     * By default, it uses a child first delegation model rather than the standard parent first.
     * If the requested class cannot be found in this class loader, the parent class loader will be consulted
     * via the standard {@link ClassLoader#loadClass(String)} mechanism.
     * Use {@link #parentFirst} to change the loading strategy.
     */
    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(className)) {
            // first check whether it's a system class, delegate to the system loader
            if (className.startsWith(JAVA_PACKAGE_PREFIX)) {
                return findSystemClass(className);
            }

            // if the class is part of the plugin engine use parent class loader
            if (className.startsWith(PLUGIN_PACKAGE_PREFIX) && !className.startsWith("org.pf4j.demo")) {
//                log.trace("Delegate the loading of PF4J class '{}' to parent", className);
                return getParent().loadClass(className);
            }

            log.trace("Received request to load class '{}'", className);

            // second check whether it's already been loaded
            Class<?> loadedClass = findLoadedClass(className);
            if (loadedClass != null) {
                log.trace("Found loaded class '{}'", className);
                return loadedClass;
            }

            if (!parentFirst) {
                // nope, try to load locally
                try {
                    loadedClass = findClass(className);
                    log.trace("Found class '{}' in plugin classpath", className);
                    return loadedClass;
                } catch (ClassNotFoundException e) {
                    // try next step
                }
                // use the standard ClassLoader (which follows normal parent delegation)

                try {
                    loadedClass = super.getParent().loadClass(className);
                    log.trace("Found class '{}' in parent classpath", className);
                    return loadedClass;
                } catch (ClassNotFoundException e) {
                    // try next step
                }

                log.trace("Couldn't find class '{}' in plugin or parents classpath. Delegating to dependencies", className);

                // look in dependencies


            } else {
                // try to load from parent
                try {
                    return super.loadClass(className);
                } catch (ClassNotFoundException e) {
                    // try next step
                }

                log.trace("Couldn't find class '{}' in parent. Delegating to plugin classpath", className);

                // nope, try to load locally
                try {
                    loadedClass = findClass(className);
                    log.trace("Found class '{}' in plugin classpath", className);
                    return loadedClass;
                } catch (ClassNotFoundException e) {
                    // try next step
                }

                // look in dependencies

            }
            loadedClass = loadClassFromDependencies(className);
            if (loadedClass != null) {
                log.trace("Found class '{}' in dependencies", className);
                return loadedClass;
            }
            throw new ClassNotFoundException(className+"@"+getName());
        }
    }

    @Override
    protected Class<?> loadClassFromDependencies(String className) {
        log.trace("Search in dependencies for class '{}'", className);
        List<PluginDependency> dependencies = pluginDescriptor.getDependencies();
        for (PluginDependency dependency : dependencies) {
            ClassLoader classLoader = pluginManager.getPluginClassLoader(dependency.getPluginId());

            // If the dependency is marked as optional, its class loader might not be available.
            if (classLoader == null && dependency.isOptional()) {
                continue;
            }

            try {
                return classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                // try next dependency
            }
        }

        return null;
    }
}
