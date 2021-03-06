package com.flexicore.init;

import com.flexicore.annotations.OperationsInside;
import com.flexicore.data.jsoncontainers.CrossLoaderResolver;
import com.flexicore.events.PluginsLoadedEvent;
import com.flexicore.interceptors.SecurityImposer;
import com.flexicore.interfaces.AspectPlugin;
import com.flexicore.interfaces.Plugin;
import com.flexicore.interfaces.RestServicePlugin;
import com.flexicore.interfaces.dynamic.InvokerInfo;
import com.flexicore.rest.JaxRsActivator;
import com.flexicore.runningentities.FilesCleaner;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.ClassScannerService;
import com.flexicore.service.impl.SecurityService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.SpringDocUtils;
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
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.ws.rs.ext.Provider;
import java.lang.reflect.Method;
import java.util.*;
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
    private ClassScannerService classScannerService;


    @Autowired
    private FilesCleaner filesCleaner;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private HealthContributorRegistry healthContributorRegistry;
    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;
    private static final Set<Class<?>> springRestPluginClasses=new HashSet<>();

    /**
     * Starts plug-in threads
     */
    private void initThreads() {
        initPluginLoader();
        initFilesCleaner();

    }

    private void initFilesCleaner() {

        new Thread(filesCleaner).start();
    }


    /**
     * Starts the plug-in loader, uses a constant for its path
     * @return PluginsLoadedEvent
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public PluginsLoadedEvent initPluginLoader() {
        SecurityContext securityContext = securityService.getAdminUserSecurityContext();
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
                Class<? extends RestServicePlugin> restClass = plugin.getClass();
                try {
                        AspectJProxyFactory factory = new AspectJProxyFactory(plugin);
                        SecurityImposer securityImposer = applicationContext.getBean(SecurityImposer.class);
                        factory.addAspect(securityImposer);
                        for (AspectPlugin aspect : aspects) {
                            factory.addAspect(aspect);
                        }
                        factory.setProxyTargetClass(true);
                        Object proxy = factory.getProxy(restClass.getClassLoader());
                        if(restClass.isAnnotationPresent(RestController.class)){
                            registerSpringRESTController(proxy,restClass);
                        }
                        else{
                            JaxRsActivator.addSingletones(proxy);
                        }
                    } catch (Exception e) {
                        logger.error("Failed registering REST service " + restClass, e);
                    }




            }
            Map<String, RequestMappingHandlerMapping> beansOfType = applicationContext.getBeansOfType(RequestMappingHandlerMapping.class);
        logger.info(beansOfType+"");
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
            List<Class<? extends Plugin>> classes = pluginManager.getExtensionClasses(Plugin.class, startedPlugin.getPluginId());
            for (Class<? extends Plugin> c : classes) {
                if (c.isAnnotationPresent(Provider.class)) {
                    JaxRsActivator.addProvider(c);

                }

            /*if (c.isAnnotationPresent(ServerEndpoint.class)) {
                try {
                    serverContainer.addEndpoint(c);
                } catch (DeploymentException e) {
                    logger.error("failed adding WS",e);
                }
            }*/

                if (c.isAnnotationPresent(OperationsInside.class)) {
                    classScannerService.registerOperationsInclass(c,securityContext); // Adds
                }
                if (c.isAnnotationPresent(InvokerInfo.class)) {
                    classScannerService.registerInvoker(c, securityContext);
                }
                if (c.isAnnotationPresent(OpenAPIDefinition.class)) {
                    classScannerService.addSwaggerTags(c, securityContext);
                }
                Tag[] annotationsByType = c.getAnnotationsByType(Tag.class);
                if (annotationsByType.length != 0) {
                    classScannerService.addSwaggerTags(c, securityContext);
                }
            }
            logger.debug("registering "+startedPlugin.getPluginId() +" for basic services took "+(System.currentTimeMillis()-start));

        }
        logger.debug("registering plugins for basic services took "+(System.currentTimeMillis()-startedAll));


        PluginsLoadedEvent event = new PluginsLoadedEvent(applicationContext, startedPlugins);
        eventPublisher.publishEvent(event);
        return event;


    }

    private void registerSpringRESTController(Object plugin,Class<?> originalClass) {
        CustomRequestMappingHandlerMapping requestMappingHandlerMapping= (CustomRequestMappingHandlerMapping) this.requestMappingHandlerMapping;
        Class<?> pluginClass = plugin.getClass();
        for (Method method : originalClass.getMethods()) {
            RequestMappingInfo mappingForMethod = requestMappingHandlerMapping.getMappingForMethod(method, pluginClass);
            if(mappingForMethod!=null){
                requestMappingHandlerMapping.registerMapping(mappingForMethod,plugin,method);
            }

        }
        SpringDocUtils.getConfig().addRestControllers(originalClass);
        springRestPluginClasses.add(originalClass);
    }


    @EventListener
    public void onContextStopped(ContextStoppedEvent contextStoppedEvent) {
        filesCleaner.stop();
    }

    public static Set<Class<?>> getSpringRestPluginClasses() {
        return springRestPluginClasses;
    }
}
