package com.wizzdi.flexicore.init;

import com.flexicore.annotations.EnableFlexiCoreBaseServices;
import com.wizzdi.dynamic.properties.converter.DynamicPropertiesModule;
import com.wizzdi.dynamic.properties.converter.EnableDynamicProperties;
import com.wizzdi.dynamic.properties.converter.JsonConverterImplementationHolder;
import com.wizzdi.dynamic.properties.converter.postgresql.PostgresqlJsonConverter;
import com.wizzdi.flexicore.boot.base.annotations.plugins.EnableFlexiCorePlugins;
import com.wizzdi.flexicore.boot.base.init.FlexiCoreAppBeanFactory;
import com.wizzdi.flexicore.boot.dynamic.invokers.annotations.EnableDynamicInvokersPlugins;
import com.wizzdi.flexicore.boot.health.annotations.EnableFlexiCoreHealthPlugins;
import com.wizzdi.flexicore.boot.jaxrs.annotations.EnableFlexiCoreJAXRSPlugins;
import com.wizzdi.flexicore.boot.jpa.annotations.EnableFlexiCoreJPAPlugins;
import com.wizzdi.flexicore.boot.rest.annotations.EnableFlexiCoreRESTPlugins;
import com.wizzdi.flexicore.boot.swagger.annotations.EnableFlexiCoreSwaggerPlugins;
import com.wizzdi.flexicore.boot.websockets.annotations.EnableFlexiCoreWebSocketPlugins;
import com.wizzdi.flexicore.security.annotations.EnableFlexiCoreSecurity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.IOException;
import java.util.Arrays;

@SpringBootApplication(scanBasePackageClasses = {FlexiCoreApplication.class, PostgresqlJsonConverter.class, DynamicPropertiesModule.class})
@EnableFlexiCorePlugins
@EnableFlexiCoreRESTPlugins
@EnableFlexiCoreHealthPlugins
@EnableFlexiCoreJPAPlugins
@EnableFlexiCoreWebSocketPlugins
@EnableFlexiCoreJAXRSPlugins
@EnableFlexiCoreBaseServices
@EnableFlexiCoreSecurity
@EnableMongoRepositories
@EnableDynamicInvokersPlugins
@EnableFlexiCoreSwaggerPlugins
@EnableTransactionManagement(proxyTargetClass = true)
@EnableDynamicProperties
public class FlexiCoreApplication {


	public static void main(String[] args) throws IOException {
		com.wizzdi.flexicore.boot.base.init.FlexiCoreApplication app = new com.wizzdi.flexicore.boot.base.init.FlexiCoreApplication(FlexiCoreApplication.class);


		app.addListeners(new ApplicationPidFileWriter());
		ConfigurableApplicationContext context=app.run(args);

	}

@Autowired
private JsonConverterImplementationHolder jsonConverterImplementationHolder;

	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {

			System.out.println("Let's inspect the beans provided by Spring Boot:");

			String[] beanNames = ctx.getBeanDefinitionNames();
			Arrays.sort(beanNames);
			for (String beanName : beanNames) {
				System.out.println(beanName);
			}
			System.out.println("total of "+beanNames.length +" beans");



		};
	}
}
