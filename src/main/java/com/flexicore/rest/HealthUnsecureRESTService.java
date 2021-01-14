package com.flexicore.rest;

import com.flexicore.annotations.OperationsInside;
import com.flexicore.interfaces.RESTService;
import com.flexicore.response.HealthStatusResponse;
import com.flexicore.service.impl.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.MediaType;

@Path("/healthUnsecure")
@RequestScoped
@OperationsInside
@Tag(name = "Core")
@Tag(name = "healthUnsecure")
@Component
public class HealthUnsecureRESTService implements RESTService {

    @Autowired
    private HealthService healthService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "health", description = "health")
    public HealthStatusResponse healthCheck() {
        return healthService.healthCheck();

    }


    @GET
    @Path("healthOrFail")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "healthOrFail", description = "healthOrFail")
    public boolean healthOrFail() {
        boolean down= healthService.getHealth().getStatus() == Status.DOWN;
        if(down){
            throw new ServiceUnavailableException("Health check failed");
        }
       return true;
    }


}
