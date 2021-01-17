package com.wizzdi.flexicore.init;

import com.coxautodev.graphql.tools.GraphQLResolver;
import com.wizzdi.flexicore.boot.base.init.FlexiCorePluginManager;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class FlexiCoreGraphQLResolvers {

	@Lazy
	@Autowired
	private FlexiCorePluginManager pluginManager;

	@Bean
	public GraphQLResolver<?> graphQLResolvers(){
		for (PluginWrapper startedPlugin : pluginManager.getStartedPlugins()) {

			ApplicationContext applicationContext=pluginManager.getApplicationContext(startedPlugin);
			return applicationContext.getBeansOfType(GraphQLResolver.class).values().stream().findFirst().map(f->(GraphQLResolver<?>)f).orElse(null);

		}
		return null;
	}
}
