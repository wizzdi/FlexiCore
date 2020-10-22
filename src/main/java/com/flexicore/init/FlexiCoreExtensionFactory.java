package com.flexicore.init;

import org.pf4j.PluginWrapper;
import org.pf4j.spring.SpringExtensionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class FlexiCoreExtensionFactory extends SpringExtensionFactory {

    private final FlexiCorePluginManager pluginManager;
    private final Map<String, FlexiCoreApplicationContext> contextCache = new ConcurrentHashMap<>();
    private final Queue<ApplicationContext> pluginsApplicationContexts = new LinkedBlockingQueue<>();
    private final Logger logger = LoggerFactory.getLogger(FlexiCoreExtensionFactory.class);


    public FlexiCoreExtensionFactory(FlexiCorePluginManager pluginManager) {
        super(pluginManager, true);
        this.pluginManager = pluginManager;
    }
    protected <T> T createWithoutSpring(Class<?> extensionClass) {
        try {
            Class<T> c=(Class<T>) extensionClass;
            Constructor<T> constructor = c.getConstructor();
            return constructor.newInstance();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    @Override
    public <T> T create(Class<T> extensionClass) {
        T extension = this.createWithoutSpring(extensionClass);
        if (extension != null) {
            PluginWrapper pluginWrapper = this.pluginManager.whichPlugin(extensionClass);
            ApplicationContext pluginContext = getApplicationContext(pluginWrapper);
            pluginContext.getAutowireCapableBeanFactory().autowireBean(extension);


        }

        return extension;

    }


    public ApplicationContext getApplicationContext(PluginWrapper pluginWrapper) {
        String pluginId = pluginWrapper!=null?pluginWrapper.getPluginId():"core-extensions";
        FlexiCoreApplicationContext applicationContext = contextCache.get(pluginId);
        if (applicationContext == null) {
            long start=System.currentTimeMillis();
            applicationContext = createApplicationContext(pluginWrapper);
            contextCache.put(pluginId, applicationContext);
            List<String> dependencies = pluginWrapper!=null?pluginWrapper.getDescriptor().getDependencies().parallelStream().map(f -> f.getPluginId()).sorted().collect(Collectors.toList()):new ArrayList<>();
            List<ApplicationContext> dependenciesContexts=dependencies.stream().map(f->pluginManager.getPlugin(f)).filter(f->f!=null).map(this::getApplicationContext).collect(Collectors.toList());
            applicationContext.getAutowireCapableBeanFactory().addDependenciesContext(dependenciesContexts);
            applicationContext.refresh();
            pluginsApplicationContexts.add(applicationContext);
            logger.debug("creating context for "+pluginId +" took "+(System.currentTimeMillis()-start)+"ms");

        }
        return applicationContext;
    }

    private FlexiCoreApplicationContext createApplicationContext(PluginWrapper pluginWrapper) {
        String pluginId = pluginWrapper!=null?pluginWrapper.getPluginId():null;
        List<Class<? extends com.flexicore.interfaces.Plugin>> beanClasses = pluginManager.getExtensionClasses(com.flexicore.interfaces.Plugin.class, pluginId);
        ClassLoader pluginClassLoader = pluginWrapper!=null?pluginWrapper.getPluginClassLoader():Thread.currentThread().getContextClassLoader();
        FlexiCoreApplicationContext applicationContext = new FlexiCoreApplicationContext();
        applicationContext.setParent(pluginManager.getApplicationContext());
        applicationContext.setClassLoader(pluginClassLoader);
        for (Class<? extends com.flexicore.interfaces.Plugin> beanClass : beanClasses) {
            applicationContext.register(beanClass);
        }

        return applicationContext;

    }



    public Queue<ApplicationContext> getPluginsApplicationContexts() {
        return pluginsApplicationContexts;
    }

}
