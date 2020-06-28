package com.flexicore.init;

import org.pf4j.JarPluginLoader;
import org.pf4j.PluginClassLoader;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginManager;

import java.nio.file.Path;

public class FlexiCoreJarPluginLoader extends JarPluginLoader {

    public FlexiCoreJarPluginLoader(PluginManager pluginManager) {
        super(pluginManager);
    }




    @Override
    public ClassLoader loadPlugin(Path pluginPath, PluginDescriptor pluginDescriptor) {
        ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
        FlexiCorePluginClassLoader pluginClassLoader = new FlexiCorePluginClassLoader(pluginManager, pluginDescriptor, parentClassLoader);
        pluginClassLoader.addFile(pluginPath.toFile());


        return pluginClassLoader;
    }
}
