package com.flexicore.rest;

import com.flexicore.annotations.OperationsInside;
import com.flexicore.annotations.ProtectedREST;
import com.flexicore.interfaces.RESTService;
import com.flexicore.response.HealthStatusResponse;
import com.flexicore.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.*;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.stereotype.Component;

import javax.enterprise.context.RequestScoped;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/health")
@RequestScoped
@ProtectedREST
@OperationsInside
@Tag(name = "Core")
@Tag(name = "Health")
@Component
public class HealthRESTService implements RESTService {

    @Autowired
    private HealthEndpoint healthEndpoint;
            ;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "health", description = "health")
    public HealthStatusResponse healthCheck(
            @QueryParam("authenticationKey") String authenticationKey,
            @Context SecurityContext securityContext) {
        HealthComponent health = healthEndpoint.health();
        return new HealthStatusResponse(health);

    }



}
