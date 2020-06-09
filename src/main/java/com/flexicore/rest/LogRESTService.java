/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.rest;

import com.flexicore.annotations.IOperation;
import com.flexicore.annotations.IOperation.Access;
import com.flexicore.annotations.OperationsInside;
import com.flexicore.interceptors.SecurityImposer;
import com.flexicore.interfaces.RESTService;
import com.flexicore.request.LogCreate;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.LogService;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.enterprise.context.RequestScoped;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.flexicore.annotations.Protected;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;


@Path("/logs")
@RequestScoped
@Component
@OperationsInside
@Protected
@Tag(name = "Logs")
public class LogRESTService implements RESTService {


    @Autowired
    private LogService logService;







  

    @POST
    @Path("/log")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "log", Description = "log")
    public void log(@HeaderParam("authenticationkey") String authenticationkey
            , LogCreate logCreate, @Context SecurityContext securityContext) {
        logService.validate(logCreate,securityContext);
        logService.log(logCreate, securityContext);

    }

}
