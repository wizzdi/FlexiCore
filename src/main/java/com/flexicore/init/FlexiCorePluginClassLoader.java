package com.flexicore.init;

import org.pf4j.PluginClassLoader;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginManager;

public class FlexiCorePluginClassLoader extends PluginClassLoader {
    private String name;
    public FlexiCorePluginClassLoader(PluginManager pluginManager, PluginDescriptor pluginDescriptor, ClassLoader parent) {
        super(pluginManager, pluginDescriptor, parent);
        initName(pluginDescriptor);
    }

    private void initName(PluginDescriptor pluginDescriptor) {
        if(pluginDescriptor!=null){
            name=pluginDescriptor.getPluginId()+"@"+pluginDescriptor.getVersion();
        }
    }

    public FlexiCorePluginClassLoader(PluginManager pluginManager, PluginDescriptor pluginDescriptor, ClassLoader parent, boolean parentFirst) {
        super(pluginManager, pluginDescriptor, parent, parentFirst);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "FlexiCorePluginClassLoader{"+name+"}";
    }
}
