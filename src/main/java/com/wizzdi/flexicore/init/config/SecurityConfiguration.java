package com.wizzdi.flexicore.init.config;

import com.wizzdi.security.adapter.SecurityPathConfigurator;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.Arrays;

@Configuration
public class SecurityConfiguration {

  @Value("${flexicore.api.path:/api}")
  private String apiPrefix;
  @Value("${flexicore.swagger.url:#{null}}")
  private String swaggerURL;

  @Bean
  @Order(99)
  public SecurityPathConfigurator loginPath() {
    return f -> {
      f.requestMatchers(apiPrefix + "/login", apiPrefix + "/api/register", apiPrefix + "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
      f.requestMatchers(apiPrefix + "/**").authenticated();
      f.anyRequest().permitAll();

      return f;
    };
  }

  @Bean
  public OpenAPI customOpenAPI() {
    Server server = new Server();
    server.setUrl(swaggerURL);
    return new OpenAPI()
            .addServersItem(server)
            .components(new Components().addSecuritySchemes("bearer-jwt",
                    new io.swagger.v3.oas.models.security.SecurityScheme().type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                            .in(io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER).name("Authorization")))
            .addSecurityItem(
                    new SecurityRequirement().addList("bearer-jwt", Arrays.asList("read", "write")));
  }
}
