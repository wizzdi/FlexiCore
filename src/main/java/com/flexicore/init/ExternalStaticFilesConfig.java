package com.flexicore.init;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.io.FilenameFilter;

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
                registry.addViewController("/FlexiCore").setViewName("redirect:/FlexiCore/"); //delete these two lines if looping with directory as below
                registry.addViewController("/FlexiCore/").setViewName("forward:/FlexiCore/index.html"); //delete these two lines if looping with directory as below
                registry.addViewController("/notFound").setViewName("forward:/index.html");

                String[] directories = listDirectories(externalStatic);
                if(directories!=null){
                    for (String subDir : directories){
                        registry.addViewController("/"+subDir).setViewName("redirect:/" + subDir + "/");
                        registry.addViewController("/"+subDir+"/").setViewName("forward:/" + subDir + "/index.html");
                    }
                }

            }
        };
    }
    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> containerCustomizer() {
        return container -> {
            container.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND,
                    "/notFound"));
        };
    }

    private String[] listDirectories(String root){
        File file = new File(root);
        return file.list((current, name) -> new File(current, name).isDirectory());
    }
}