package com.flexicore.init;

import org.pf4j.*;
import org.pf4j.spring.SpringPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@Lazy
public class FlexiCorePluginManager extends SpringPluginManager {

    private static final Logger logger= LoggerFactory.getLogger(FlexiCorePluginManager.class);

    public FlexiCorePluginManager() {
    }

    public FlexiCorePluginManager(Path pluginsRoot) {
        super(pluginsRoot);
    }

    @Override
    protected ExtensionFactory createExtensionFactory() {
        return new FlexiCoreExtensionFactory(this);
    }



    public Collection<ApplicationContext> getPluginApplicationContexts(){
        FlexiCoreExtensionFactory extensionFactory = (FlexiCoreExtensionFactory) getExtensionFactory();
        return Collections.unmodifiableCollection(extensionFactory.getPluginsApplicationContexts());
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
        long start = System.currentTimeMillis();
        try {
            this.loadPlugins();
        }
        catch (DependencyResolver.DependenciesWrongVersionException e){
            List<DependencyResolver.WrongDependencyVersion> dependencies = e.getDependencies();
            logger.error("loading plugins failed , wrong versions: "+dependencies.stream().map(f->getWrongDependencyString(f)).collect(Collectors.joining(System.lineSeparator())),e);
            throw e;
        }
        this.startPlugins();
        AbstractAutowireCapableBeanFactory beanFactory = (AbstractAutowireCapableBeanFactory) getApplicationContext().getAutowireCapableBeanFactory();
        FlexiCoreExtensionsInjector extensionsInjector = new FlexiCoreExtensionsInjector(this, beanFactory);
        extensionsInjector.injectExtensions();
        logger.debug("loading and starting plugins took "+(System.currentTimeMillis()-start)+"ms");

    }

    private String getWrongDependencyString(DependencyResolver.WrongDependencyVersion wrongDependencyVersion) {
        return "plugin "+wrongDependencyVersion.getDependentId() +" required "+wrongDependencyVersion.getDependencyId() +" version: "+wrongDependencyVersion.getRequiredVersion() +", actual: "+wrongDependencyVersion.getExistingVersion();
    }
}
