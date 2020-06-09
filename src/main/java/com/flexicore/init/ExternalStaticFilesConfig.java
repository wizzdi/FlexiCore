package com.flexicore.init;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ExternalStaticFilesConfig {
    // I assign filePath and pathPatterns using @Value annotation
    @Value("${flexicore.externalStatic}")
    private String externalStatic;
    @Value("${flexicore.externalStaticMapping}")
    private String externalStaticMapping;

    @Bean
    public WebMvcConfigurer webMvcConfigurerAdapter() {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/FlexiCore/**")
                        .addResourceLocations("classpath:/static/");
                registry.addResourceHandler(externalStaticMapping)
                        .addResourceLocations("file:" + externalStatic);
            }

            @Override
            public void addViewControllers(ViewControllerRegistry registry) {
                registry.addViewController("/FlexiCore").setViewName("forward:/FlexiCore/index.html");
            }
        };
    }
}