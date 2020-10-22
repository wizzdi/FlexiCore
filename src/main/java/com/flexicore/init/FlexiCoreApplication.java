package com.flexicore.init;


import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import java.util.Arrays;

@SpringBootApplication
@ComponentScan(basePackages = "com.flexicore")
@EntityScan(basePackages = {"com.flexicore.model"})
@EnableWebSocket
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

        };
    }
}
