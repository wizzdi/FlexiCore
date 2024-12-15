package com.wizzdi.flexicore.init.config;

import com.wizzdi.flexicore.boot.rest.interfaces.ApiPathChangeExclusion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class WebMvcConfig {

    @Bean
    public ApiPathChangeExclusion apiPathChangeExclusion() {
        return new ApiPathChangeExclusion().setExcludedPackages(Set.of("org.springdoc","org.spring"));
    }

}
