package com.wizzdi.flexicore.init.config;

import com.wizzdi.security.adapter.SecurityPathConfigurator;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.Arrays;

@Configuration
public class SecurityConfiguration {

  @Bean
  @Order(99)
  public SecurityPathConfigurator loginPath() {
    return expressionInterceptUrlRegistry ->
        expressionInterceptUrlRegistry
            .requestMatchers(
                "/login", "/register", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
            .permitAll();
  }

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearer-jwt",
                    new io.swagger.v3.oas.models.security.SecurityScheme()
                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER)
                        .name("Authorization")))
        .addSecurityItem(
            new SecurityRequirement().addList("bearer-jwt", Arrays.asList("read", "write")));
  }
}
