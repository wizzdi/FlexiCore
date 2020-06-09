package com.flexicore.rest.interfaces;

import com.flexicore.annotations.Audit;
import com.flexicore.annotations.IOperation;
import com.flexicore.interfaces.RESTService;
import com.flexicore.security.AuthenticationBundle;
import com.flexicore.security.AuthenticationRequestHolder;
import com.flexicore.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/authentication")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)

public interface IAuthenticationRESTService extends RESTService {
    @POST
    @Path("/login")
    @IOperation(access = IOperation.Access.allow, Name = "login", Description = "Log into the system")
    @Operation(summary = "Sign-in(login)", description = "Sign-in into the system, pass an initialized instance of AuthenticationRequestHolder, sign-in name and password are stored there")
    @Audit(auditType = "Login")
    AuthenticationBundle login(@HeaderParam("authenticationkey") String authenticationkey,
                               AuthenticationRequestHolder bundle);

    @POST
    @Path("/logout")
    @Consumes(MediaType.APPLICATION_JSON)
    @IOperation(access = IOperation.Access.allow, Name = "logout", Description = "Log out from the system")
    @Operation(summary = "Sign-out(logout)", description = "log-out from the system")
    @Audit(auditType = "Logout")
    boolean logout(@HeaderParam("authenticationkey") String authenticationkey,
                   @Context SecurityContext securityContext);
}
