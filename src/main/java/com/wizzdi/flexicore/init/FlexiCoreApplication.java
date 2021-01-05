package com.wizzdi.flexicore.init;

import com.flexicore.annotations.EnableFlexiCoreBaseServices;
import com.wizzdi.flexicore.boot.base.annotations.plugins.EnableFlexiCorePlugins;
import com.wizzdi.flexicore.boot.health.annotations.EnableFlexiCoreHealthPlugins;
import com.wizzdi.flexicore.boot.jaxrs.annotations.EnableFlexiCoreJAXRSPlugins;
import com.wizzdi.flexicore.boot.jpa.annotations.EnableFlexiCoreJPAPlugins;
import com.wizzdi.flexicore.boot.rest.annotations.EnableFlexiCoreRESTPlugins;
import com.wizzdi.flexicore.boot.websockets.annotations.EnableFlexiCoreWebSocketPlugins;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

@SpringBootApplication
@EnableFlexiCorePlugins
@EnableFlexiCoreRESTPlugins
@EnableFlexiCoreHealthPlugins
@EnableFlexiCoreJPAPlugins
@EnableFlexiCoreWebSocketPlugins
@EnableFlexiCoreJAXRSPlugins
@EnableFlexiCoreBaseServices
public class FlexiCoreApplication {




	public static void main(String[] args) {


		SpringApplication app = new SpringApplication(FlexiCoreApplication.class);
		app.addListeners(new ApplicationPidFileWriter());
		ConfigurableApplicationContext context=app.run(args);

	}

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
