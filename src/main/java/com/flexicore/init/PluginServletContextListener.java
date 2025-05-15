package com.flexicore.init;

import com.flexicore.events.PluginsLoadedEvent;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PluginServletContextListener implements ServletContextInitializer {
	private static final Logger logger = LoggerFactory.getLogger(PluginServletContextListener.class);
	@Autowired
	private PluginsLoadedEvent pluginsLoadedEvent;
	@Autowired
	private PluginManager pluginManager;

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		for (PluginWrapper startedPlugin : pluginsLoadedEvent.getStartedPlugins()) {

			ClassLoader pluginClassLoader = startedPlugin.getPluginClassLoader();
			List<ServletContextInitializer> servletContextInitializers = ((FlexiCorePluginManager) pluginManager).getApplicationContext(startedPlugin).getBeansOfType(ServletContextInitializer.class).values().stream().collect(Collectors.toList());
			for (ServletContextInitializer servletContextInitializer : servletContextInitializers) {
				logger.info("registering servlet {} from plugin {}",servletContextInitializer.getClass().getCanonicalName() ,startedPlugin.getPluginId());
				ClassLoader originalThreadContextClassLoader = Thread.currentThread().getContextClassLoader();
				try {

					Thread.currentThread().setContextClassLoader(pluginClassLoader);
					servletContextInitializer.onStartup(servletContext);
					if (servletContextInitializer instanceof ServletRegistrationBean) {
						registerTcclFilterForPluginServlet(servletContext, (ServletRegistrationBean<?>) servletContextInitializer, pluginClassLoader, startedPlugin.getPluginId());
					}

				} catch (ServletException e) {
					logger.error("failed starting servlet context init");
				} finally {
					Thread.currentThread().setContextClassLoader(originalThreadContextClassLoader);
				}
			}
		}

	}
	private void registerTcclFilterForPluginServlet(ServletContext servletContext,
													ServletRegistrationBean<?> servletRegistrationBean,
													ClassLoader pluginClassLoader,
													String pluginId) {
		if (servletRegistrationBean.getUrlMappings().isEmpty()) {
			logger.warn("ServletRegistrationBean '{}' from plugin '{}' has no URL mappings. Skipping TCCL filter registration.",
					servletRegistrationBean.getServletName(), pluginId);
			return;
		}

		String filterName = pluginId + "-" + servletRegistrationBean.getServletName() + "-TcclFilter";
		logger.info("Auto-registering TCCL filter '{}' for servlet '{}' from plugin '{}'",
				filterName, servletRegistrationBean.getServletName(), pluginId);

		PluginTCCLFilter tcclFilter = new PluginTCCLFilter(pluginClassLoader);
		FilterRegistration.Dynamic filterRegistration = servletContext.addFilter(filterName, tcclFilter);

		if (filterRegistration == null) {
			logger.warn("Failed to register TCCL filter '{}' dynamically. It might already exist. Skipping.", filterName);
			return; // Filter with this name might have been registered by another means or a previous attempt
		}

		// Map the filter to the same URL patterns as the servlet
		// Ensure it runs for all relevant dispatcher types (REQUEST, ASYNC, etc.)
		filterRegistration.addMappingForUrlPatterns(
				EnumSet.of(javax.servlet.DispatcherType.REQUEST, javax.servlet.DispatcherType.ASYNC),
				false, // isMatchAfter
				servletRegistrationBean.getUrlMappings().toArray(new String[0])
		);
		filterRegistration.setAsyncSupported(true); // Generally good practice

		logger.info("Successfully registered and mapped TCCL filter '{}' for plugin '{}' to patterns: {}",
				filterName, pluginId, servletRegistrationBean.getUrlMappings());
	}
	public static class PluginTCCLFilter implements Filter {
		private final ClassLoader pluginClassLoader;

		public PluginTCCLFilter(ClassLoader pluginClassLoader) {
			if (pluginClassLoader == null) {
				throw new IllegalArgumentException("Plugin ClassLoader cannot be null for PluginTCCLFilter");
			}
			this.pluginClassLoader = pluginClassLoader;
		}

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
			// No-op for this simple filter
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
			ClassLoader originalThreadContextClassLoader = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(this.pluginClassLoader);
				chain.doFilter(request, response);
			} finally {
				Thread.currentThread().setContextClassLoader(originalThreadContextClassLoader);
			}
		}

		@Override
		public void destroy() {
			// No-op
		}
	}
}
