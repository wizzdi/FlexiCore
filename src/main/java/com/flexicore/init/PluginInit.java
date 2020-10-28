package com.flexicore.init;

import com.flexicore.data.jsoncontainers.CrossLoaderResolver;
import com.flexicore.events.PluginsLoadedEvent;
import com.flexicore.interfaces.*;
import com.flexicore.rest.JaxRsActivator;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import javax.ws.rs.ext.Provider;
import java.lang.reflect.Constructor;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

@Configuration
public class PluginInit {

    private static final Logger logger = LoggerFactory.getLogger(PluginInit.class);
    private static final Comparator<PluginWrapper> PLUGIN_COMPARATOR_FOR_REST = Comparator.comparing(f -> f.getDescriptor().getVersion());

    @Autowired
    @Lazy
    private PluginManager pluginManager;
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private HealthContributorRegistry healthContributorRegistry;
    private static Set<String> handledContext = new ConcurrentSkipListSet<>();



    /**
     * Starts the plug-in loader, uses a constant for its path
     * @return PluginsLoadedEvent
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public PluginsLoadedEvent initPluginLoader() {
        CrossLoaderResolver.registerClassLoader( pluginManager.getStartedPlugins().stream().map(f->f.getPluginClassLoader()).collect(Collectors.toList()));
        List<PluginWrapper> startedPlugins = pluginManager.getStartedPlugins().stream().sorted(PLUGIN_COMPARATOR_FOR_REST).collect(Collectors.toList());
        List<? extends AspectPlugin> aspects = pluginManager.getExtensions(AspectPlugin.class);

        long startedAll=System.currentTimeMillis();
        //makes sure older versions are loaded first so default version for apis is being taken from oldest plugin
        for (PluginWrapper startedPlugin : startedPlugins) {
            long start = System.currentTimeMillis();
            logger.info("REST Registration handling plugin: " + startedPlugin);

            List<? extends RestServicePlugin> restPlugins = pluginManager.getExtensions(RestServicePlugin.class, startedPlugin.getPluginId());
            for (RestServicePlugin plugin : restPlugins) {

                logger.info("REST class " + plugin);
                try {
                    AspectJProxyFactory factory = new AspectJProxyFactory(plugin);
                    for (AspectPlugin aspect : aspects) {
                        factory.addAspect(aspect);
                    }
                    factory.setProxyTargetClass(true);
                    Object proxy = factory.getProxy(plugin.getClass().getClassLoader());
                    JaxRsActivator.addSingletones(proxy);
                } catch (Exception e) {
                    logger.error("Failed registering REST service " + plugin.getClass(), e);
                }
            }

            List<? extends HealthContributor> healthContributors = pluginManager.getExtensions(HealthContributor.class, startedPlugin.getPluginId());
            for (HealthContributor healthContributor : healthContributors) {
                logger.info("Health class " + healthContributor.getClass().getCanonicalName());

                try {
                    healthContributorRegistry.registerContributor(healthContributor.getClass().getSimpleName(), healthContributor);
                }
                catch (Exception e){
                    logger.error("Failed registering Health " + healthContributor.getClass(), e);

                }
            }

            List<JaxRSProviderPlugin> classes = pluginManager.getExtensions(JaxRSProviderPlugin.class, startedPlugin.getPluginId());
            for (JaxRSProviderPlugin jaxRSProviderPlugin : classes) {
                JaxRsActivator.addSingletones(jaxRSProviderPlugin);
                logger.debug("registered JAX-RS provider "+jaxRSProviderPlugin);
            }



        }
        logger.debug("registering plugins for basic services took "+(System.currentTimeMillis()-startedAll));


        PluginsLoadedEvent event = new PluginsLoadedEvent(applicationContext, startedPlugins);
        eventPublisher.publishEvent(event);
        return event;


    }



}
