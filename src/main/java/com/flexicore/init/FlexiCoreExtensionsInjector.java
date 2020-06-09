package com.flexicore.init;

import org.pf4j.PluginManager;
import org.pf4j.spring.ExtensionsInjector;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;

public class FlexiCoreExtensionsInjector extends ExtensionsInjector {

    public FlexiCoreExtensionsInjector(PluginManager pluginManager, AbstractAutowireCapableBeanFactory beanFactory) {
        super(pluginManager, beanFactory);
    }

    @Override
    protected void registerExtension(Class<?> extensionClass) {
        Object extension = this.pluginManager.getExtensionFactory().create(extensionClass);

    }
}
