package com.flexicore.init;

import com.flexicore.data.jsoncontainers.CrossLoaderResolver;
import com.flexicore.events.PluginsLoadedEvent;
import com.flexicore.interfaces.*;
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
import org.springframework.context.annotation.Scope;

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

        long startedAll=System.currentTimeMillis();
        //makes sure older versions are loaded first so default version for apis is being taken from oldest plugin
        for (PluginWrapper startedPlugin : startedPlugins) {
            long start = System.currentTimeMillis();

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



        }
        logger.debug("registering plugins for basic services took "+(System.currentTimeMillis()-startedAll));


        PluginsLoadedEvent event = new PluginsLoadedEvent(applicationContext, startedPlugins);
        eventPublisher.publishEvent(event);
        return event;


    }



}
