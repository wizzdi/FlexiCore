package com.flexicore.init;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
@DependsOn("customSpringPluginConfigurator")
public class WSExporter {


    @Bean
    public CustomSpringConfigurator customSpringConfigurator() {
        return new CustomSpringConfigurator(); // This is just to get context
    }

    @Bean
    public ServerEndpointExporter endpointExporter() {
        return new FlexiCoreEndpointExporter();
    }
}
