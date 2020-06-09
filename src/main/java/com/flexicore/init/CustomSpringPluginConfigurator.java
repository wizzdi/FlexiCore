package com.flexicore.init;

import com.flexicore.interceptors.DynamicResourceInjector;
import com.flexicore.interceptors.SecurityImposer;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.websocket.server.ServerEndpointConfig;

@Component
public class CustomSpringPluginConfigurator extends ServerEndpointConfig.Configurator implements ApplicationContextAware {


    /**
     * Spring application context.
     */
    private static volatile BeanFactory context;

    @Override
    public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
        FlexiCorePluginManager pluginManager = context.getBean(FlexiCorePluginManager.class);
        ApplicationContext applicationContext = pluginManager.getApplicationContext(clazz);
        T bean = applicationContext.getBean(clazz);
        AspectJProxyFactory factory = new AspectJProxyFactory(bean);
        SecurityImposer securityImposer = applicationContext.getBean(SecurityImposer.class);
        factory.addAspect(securityImposer);
        DynamicResourceInjector dynamicResourceInjector = applicationContext.getBean(DynamicResourceInjector.class);
        factory.addAspect(dynamicResourceInjector);
        factory.setProxyTargetClass(true);
        return factory.getProxy(clazz.getClassLoader());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        CustomSpringPluginConfigurator.context = applicationContext;
    }

}