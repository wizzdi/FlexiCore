package com.flexicore.init;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class PluginLoaderConfig {

    @Value("${flexicore.plugins}")
    private String pluginsDir;

    @Bean
    public FlexiCorePluginManager pluginManager(StartingContext startingContext) {
        return new FlexiCorePluginManager(new File(pluginsDir).toPath());
    }



}