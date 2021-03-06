package com.flexicore.init;

import com.flexicore.interceptors.SecurityImposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.websocket.server.ServerEndpointConfig;

@Component
public class CustomSpringPluginConfigurator extends ServerEndpointConfig.Configurator implements ApplicationContextAware {

	private static final Logger logger = LoggerFactory.getLogger(CustomSpringPluginConfigurator.class);
	/**
	 * Spring application context.
	 */
	private static volatile BeanFactory context;

	@Override
	public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
		try {
			FlexiCorePluginManager pluginManager = context.getBean(FlexiCorePluginManager.class);
			ApplicationContext applicationContext = pluginManager.getApplicationContext(clazz);
			T bean = applicationContext.getBean(clazz);
			AspectJProxyFactory factory = new AspectJProxyFactory(bean);
			SecurityImposer securityImposer = applicationContext.getBean(SecurityImposer.class);
			factory.addAspect(securityImposer);
			factory.setProxyTargetClass(true);
			return factory.getProxy(clazz.getClassLoader());
		} catch (Exception e) {
			logger.error("failed getting endpoint instance",e);
			return null;
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		CustomSpringPluginConfigurator.context = applicationContext;
	}

}