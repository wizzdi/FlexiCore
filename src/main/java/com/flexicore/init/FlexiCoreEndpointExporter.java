package com.flexicore.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import javax.websocket.Decoder;
import javax.websocket.DeploymentException;
import javax.websocket.Encoder;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Component
public class FlexiCoreEndpointExporter extends ServerEndpointExporter {

    private static final String ANNOTATION_METHOD = "annotationData";
    private static final String ANNOTATIONS = "annotations";
    @Autowired
    private FlexiCorePluginManager flexiCorePluginManager;
    @Override
    protected void registerEndpoints() {
        Collection<ApplicationContext> leafApplicationContext = flexiCorePluginManager.getLeafApplicationContext();
        ApplicationContext mainContext = getApplicationContext();
        registerInContext(mainContext,true);
        for (ApplicationContext leaf : leafApplicationContext) {
            for(ApplicationContext context = leaf; context!=null&&context!= mainContext; context=context.getParent()){
                registerInContext(context,true);
            }

        }


    }

    private void registerInContext(ApplicationContext context, boolean alterConfiguration) {
        Set<Class<?>> endpointClasses = new LinkedHashSet<>();

        String[] endpointBeanNames = context.getBeanNamesForAnnotation(ServerEndpoint.class);
        for (String beanName : endpointBeanNames) {
            endpointClasses.add(context.getType(beanName));
        }

        for (Class<?> endpointClass : endpointClasses) {

            Class<?> toRegister = Enhancer.isEnhanced(endpointClass) ? endpointClass.getSuperclass() : endpointClass;
            if(alterConfiguration){
                alterConfiguration(toRegister);
            }
            this.registerEndpoint(toRegister);
        }

        Map<String, ServerEndpointConfig> endpointConfigMap = context.getBeansOfType(ServerEndpointConfig.class);
        for (ServerEndpointConfig endpointConfig : endpointConfigMap.values()) {
            registerEndpoint(endpointConfig);
        }
    }

    private void alterConfiguration(Class<?> toRegister) {
        try {
            ServerEndpoint serverEndpoint = toRegister.getAnnotation(ServerEndpoint.class);
            Method method = Class.class.getDeclaredMethod(ANNOTATION_METHOD, null);
            method.setAccessible(true);
            Object annotationData = method.invoke(toRegister);
            Field annotations = annotationData.getClass().getDeclaredField(ANNOTATIONS);
            annotations.setAccessible(true);
            Map<Class<? extends Annotation>, Annotation> map = (Map<Class<? extends Annotation>, Annotation>) annotations.get(annotationData);
            ServerEndpoint newServerEndpoint = new ServerEndpoint() {

                @Override
                public String value() {
                    return serverEndpoint.value();
                }

                @Override
                public String[] subprotocols() {
                    return serverEndpoint.subprotocols();
                }

                @Override
                public Class<? extends Decoder>[] decoders() {
                    return serverEndpoint.decoders();
                }

                @Override
                public Class<? extends Encoder>[] encoders() {
                    return serverEndpoint.encoders();
                }

                @Override
                public Class<? extends ServerEndpointConfig.Configurator> configurator() {
                    return CustomSpringPluginConfigurator.class;
                }

                @Override
                public Class<? extends Annotation> annotationType() {
                    return ServerEndpoint.class;
                }
            };
            map.put(ServerEndpoint.class, newServerEndpoint);
        }
        catch (Exception e){
            logger.error("unable to alter configuration",e);
        }
    }

    private void registerEndpoint(Class<?> endpointClass) {
        ServerContainer serverContainer = getServerContainer();
        Assert.state(serverContainer != null,
                "No ServerContainer set. Most likely the server's own WebSocket ServletContainerInitializer " +
                        "has not run yet. Was the Spring ApplicationContext refreshed through a " +
                        "org.springframework.web.context.ContextLoaderListener, " +
                        "i.e. after the ServletContext has been fully initialized?");
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Registering @ServerEndpoint class: " + endpointClass);
            }
            serverContainer.addEndpoint(endpointClass);
        }
        catch (DeploymentException ex) {
            throw new IllegalStateException("Failed to register @ServerEndpoint class: " + endpointClass, ex);
        }
    }
    private void registerEndpoint(ServerEndpointConfig endpointConfig) {
        ServerContainer serverContainer = getServerContainer();
        Assert.state(serverContainer != null, "No ServerContainer set");
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Registering ServerEndpointConfig: " + endpointConfig);
            }
            serverContainer.addEndpoint(endpointConfig);
        }
        catch (DeploymentException ex) {
            throw new IllegalStateException("Failed to register ServerEndpointConfig: " + endpointConfig, ex);
        }
    }
}
