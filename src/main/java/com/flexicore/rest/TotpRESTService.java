/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.rest;

import com.flexicore.annotations.OperationsInside;
import com.flexicore.annotations.Protected;
import com.flexicore.interfaces.RESTService;
import com.flexicore.request.*;
import com.flexicore.response.FinishTotpSetupResponse;
import com.flexicore.response.SetupTotpResponse;
import com.flexicore.response.TotpAuthenticationResponse;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.TotpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;


@Path("/totp")
@RequestScoped
@Component
@OperationsInside
@Protected
@Tag(name = "Totp")

public class TotpRESTService implements RESTService {

    private static final Logger logger = LoggerFactory.getLogger(TotpRESTService.class);

    @Autowired
    private TotpService totpService;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("disableTotp")
    @Operation(description = "disableTotp", summary = "disable setupTotp")
    public void disableTotp(@HeaderParam("authenticationkey") String authenticationkey,
                                                  @Context SecurityContext securityContext) {
        DisableTotpRequest disableTotpRequest = new DisableTotpRequest();
        totpService.validate(disableTotpRequest, securityContext);
        totpService.disableTotp(disableTotpRequest, securityContext);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("finishSetupTotp")
    @Operation(description = "finishSetupTotp", summary = "finish setupTotp")
    public FinishTotpSetupResponse finishSetupTotp(@HeaderParam("authenticationkey") String authenticationkey,
                                                   FinishTotpSetupRequest finishSetupTotp, @Context SecurityContext securityContext) {
        totpService.validate(finishSetupTotp, securityContext);
        return totpService.finishSetupTotp(finishSetupTotp, securityContext);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("recoverTotp")
    @Operation(description = "recoverTotp", summary = "recoverTotp")
    public TotpAuthenticationResponse recoverTotp(@HeaderParam("authenticationkey") String authenticationkey,
                                                  RecoverTotpRequest recoverTotpRequest, @Context SecurityContext securityContext) {
        totpService.validate(recoverTotpRequest, securityContext);
        return totpService.recoverTotp(recoverTotpRequest, securityContext);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("authenticateTotp")
    @Operation(description = "authenticateTotp", summary = "authenticateTotp")
    public TotpAuthenticationResponse authenticateTotp(@HeaderParam("authenticationkey") String authenticationkey,
                                                       TotpAuthenticationRequest totpAuthenticationRequest, @Context SecurityContext securityContext) {
        totpService.validate(totpAuthenticationRequest, securityContext);
        return totpService.authenticateTotp(totpAuthenticationRequest, securityContext);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("setupTotp")
    @Operation(description = "setupTotp", summary = "setupTotp")
    public SetupTotpResponse setupTotp(@HeaderParam("authenticationkey") String authenticationkey
            , @Context SecurityContext securityContext) {
        StartTotpSetup startTotpSetup = new StartTotpSetup()
                .setUser(securityContext.getUser());
        return totpService.setupTotp(startTotpSetup);
    }


}
