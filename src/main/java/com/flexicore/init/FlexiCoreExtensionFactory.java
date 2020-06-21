package com.flexicore.init;

import org.pf4j.PluginWrapper;
import org.pf4j.spring.SpringExtensionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class FlexiCoreExtensionFactory extends SpringExtensionFactory {
    private FlexiCorePluginManager pluginManager;
    private Map<String, ApplicationContext> contextCache = new ConcurrentHashMap<>();
    private Queue<ApplicationContext> leafContexts = new LinkedBlockingQueue<>();
    private Logger logger = LoggerFactory.getLogger(FlexiCoreExtensionFactory.class);


    public FlexiCoreExtensionFactory(FlexiCorePluginManager pluginManager) {
        super(pluginManager, true);
        this.pluginManager = pluginManager;
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
        ApplicationContext applicationContext = contextCache.get(pluginId);
        if (applicationContext == null) {
            applicationContext = createApplicationContext(pluginWrapper);
            contextCache.put(pluginId, applicationContext);
        }
        return applicationContext;
    }

    private ApplicationContext createApplicationContext(PluginWrapper pluginWrapper) {
        ApplicationContext parentContext = getParentContext(pluginWrapper);
        return createGenericApplicationContext(pluginWrapper, parentContext);

    }

    private ApplicationContext createGenericApplicationContext(PluginWrapper pluginWrapper, ApplicationContext parent) {
        String pluginId = pluginWrapper!=null?pluginWrapper.getPluginId():null;
        List<Class<? extends com.flexicore.interfaces.Plugin>> beanClasses = pluginManager.getExtensionClasses(com.flexicore.interfaces.Plugin.class, pluginId);
        ClassLoader pluginClassLoader = pluginWrapper!=null?pluginWrapper.getPluginClassLoader():Thread.currentThread().getContextClassLoader();
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.setParent(parent);
        applicationContext.setClassLoader(pluginClassLoader);
        for (Class<? extends com.flexicore.interfaces.Plugin> beanClass : beanClasses) {
            applicationContext.register(beanClass);
        }
        applicationContext.refresh();
        leafContexts.remove(parent);
        leafContexts.add(applicationContext);

        return applicationContext;
    }


    private ApplicationContext getParentContext(PluginWrapper pluginWrapper) {

        List<String> dependencies = pluginWrapper!=null?pluginWrapper.getDescriptor().getDependencies().parallelStream().map(f -> f.getPluginId()).sorted().collect(Collectors.toList()):new ArrayList<>();
        if (dependencies.isEmpty()) {
            return pluginManager.getApplicationContext();
        }
        String cacheKey = dependencies.stream().collect(Collectors.joining("|"));
        ApplicationContext parentContext = contextCache.get(cacheKey);
        if (parentContext == null) {
            parentContext = createParentContext(pluginWrapper, dependencies);
            contextCache.put(cacheKey, parentContext);
        }
        return parentContext;
    }

    private ApplicationContext createParentContext(PluginWrapper pluginWrapper, List<String> dependencies) {
        List<AnnotationConfigApplicationContext> parents = dependencies.stream().map(f -> (AnnotationConfigApplicationContext) getApplicationContext(pluginManager.getPlugin(f))).collect(Collectors.toList());
        leafContexts.removeAll(parents);
        if (parents.size() == 1) {
            return parents.get(0);
        }
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.setParent(pluginManager.getApplicationContext());
        applicationContext.setClassLoader(pluginWrapper.getPluginClassLoader());
        for (AnnotationConfigApplicationContext parent : parents) {
            for (String beanDefinitionName : parent.getBeanDefinitionNames()) {
                Class<?> c = parent.getType(beanDefinitionName);
                if (c != null) {
                    PluginWrapper wrapper = pluginManager.whichPlugin(c);
                    if (wrapper != null) {
                        applicationContext.registerBeanDefinition(beanDefinitionName, parent.getBeanDefinition(beanDefinitionName));
                    }

                }


            }
        }

        applicationContext.refresh();
        return applicationContext;
    }

    public Queue<ApplicationContext> getLeafContexts() {
        return leafContexts;
    }
}
