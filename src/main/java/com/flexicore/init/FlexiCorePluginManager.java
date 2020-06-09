package com.flexicore.init;

import org.pf4j.*;
import org.pf4j.spring.SpringPlugin;
import org.pf4j.spring.SpringPluginManager;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

public class FlexiCorePluginManager extends SpringPluginManager {

    public FlexiCorePluginManager() {
    }

    public FlexiCorePluginManager(Path pluginsRoot) {
        super(pluginsRoot);
    }

    @Override
    protected ExtensionFactory createExtensionFactory() {
        return new FlexiCoreExtensionFactory(this);
    }



    public Collection<ApplicationContext> getLeafApplicationContext(){
        FlexiCoreExtensionFactory extensionFactory = (FlexiCoreExtensionFactory) getExtensionFactory();
        return Collections.unmodifiableCollection(extensionFactory.getLeafContexts());
    }

    public ApplicationContext getApplicationContext(Class<?> c){
        FlexiCoreExtensionFactory extensionFactory = (FlexiCoreExtensionFactory) getExtensionFactory();
        PluginWrapper pluginWrapper=whichPlugin(c);
        return pluginWrapper==null?getApplicationContext():extensionFactory.getApplicationContext(pluginWrapper);
    }


    @Override
    protected PluginDescriptorFinder createPluginDescriptorFinder() {
        return new ManifestPluginDescriptorFinder();
    }

    @Override
    protected PluginLoader createPluginLoader() {
        return new CompoundPluginLoader()
                .add(new DevelopmentPluginLoader(this), this::isDevelopment)
                .add(new FlexiCoreJarPluginLoader(this), this::isNotDevelopment)
                .add(new DefaultPluginLoader(this), this::isNotDevelopment);
    }

    @Override
    public void init() {
        this.loadPlugins();
        this.startPlugins();
        AbstractAutowireCapableBeanFactory beanFactory = (AbstractAutowireCapableBeanFactory) getApplicationContext().getAutowireCapableBeanFactory();
        FlexiCoreExtensionsInjector extensionsInjector = new FlexiCoreExtensionsInjector(this, beanFactory);
        extensionsInjector.injectExtensions();
    }
}
