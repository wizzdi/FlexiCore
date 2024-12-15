package com.wizzdi.flexicore.init.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

  @Value("${cors.enabled:false}")
  private boolean enabled;

  @Value("${cors.mappings:/**}")
  private String[] mappings;

  @Value("${cors.allowed.origins:*}")
  private String[] allowedOrigins;

  @Value("${cors.allowed.methods:*}")
  private String[] allowedMethods;

  @Value("${cors.allowed.headers:*}")
  private String[] allowedHeaders;

  @Value("${cors.exposed.headers:*}")
  private String[] exposedHeaders;

  @Value("${cors.allowCredentials:true}")
  private boolean allowCredentials;

  @Value("${cors.maxAge:1800}")
  private long maxAge;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    if (!enabled) {
      return;
    }
    for (String mapping : mappings) {
      registry
          .addMapping(mapping)
          .allowedOriginPatterns(allowedOrigins)
          .allowedMethods(allowedMethods)
          .allowedHeaders(allowedHeaders)
          .exposedHeaders(exposedHeaders)
          .allowCredentials(allowCredentials)
          .maxAge(maxAge);
    }
  }
}
