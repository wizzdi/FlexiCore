/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.rest;

import com.flexicore.annotations.Audit;
import com.flexicore.annotations.IOperation;
import com.flexicore.annotations.IOperation.Access;
import com.flexicore.annotations.OperationsInside;
import com.flexicore.exceptions.CheckYourCredentialsException;
import com.flexicore.exceptions.UserNotFoundException;
import com.flexicore.interfaces.RESTService;
import com.flexicore.security.AuthenticationBundle;
import com.flexicore.security.AuthenticationRequestHolder;
import com.flexicore.security.RunningUser;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * no need to intercept for security here.
 *
 * @author Avishay Ben Natan
 */
@RequestScoped
@Component
@OperationsInside
@Tag(name = "Authentication")
@Path("/authentication")
public class AuthenticationRESTService implements RESTService {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationRESTService.class);
    @Autowired
    private UserService userservice;

    /**
     * Login into the system, if successful, receives an authentication key for
     * further operations.
     *
     * @param authenticationkey authentication key - empty
     * @param bundle            authentication request
     * @param req               request context
     * @return authentication bundle
     */
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "login", Description = "Log into the system")
    @Operation(summary = "Sign-in(login)", description = "Sign-in into the system, pass an initialized instance of AuthenticationRequestHolder, sign-in name and password are stored there")
    @Audit(auditType = "Login")
    public AuthenticationBundle login(@HeaderParam("authenticationkey") String authenticationkey,
                                      @Context HttpServletRequest req,
                                      AuthenticationRequestHolder bundle) {
        String remoteIp = getRemoteIp(req);
        if (remoteIp != null) {
            bundle.setIp(remoteIp);
        }
        RunningUser runninguser = null;

        try {

            runninguser = userservice.login(bundle, null);

        } catch (UserNotFoundException | CheckYourCredentialsException e) {

            log.warn( "unable to log in", e);
        }
        if (runninguser != null) {
            AuthenticationBundle bundleRet = new AuthenticationBundle(bundle.getMail());
            bundleRet.setAuthenticationkey(runninguser.getAuthenticationkey().getKey());
            return bundleRet;

        } else {
            log.info("have failed to log user: " + bundle.getMail());
            throw new ClientErrorException(HttpResponseCodes.SC_UNAUTHORIZED);

        }

    }

    private String getRemoteIp(HttpServletRequest req) {
        String ip = req.getRemoteAddr();
        try {
            ip = req.getHeader("X-Forwarded-For").split(",")[0];
        } catch (Exception ignored) {
        }
        return ip;
    }


    /**
     * @param authenticationkey authentication key for the user
     * @param securityContext   security context obtained from auethentication key
     * @return true if the user was logged out successfully or not
     */
    @POST
    @Path("/logout")
    @Consumes(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "logout", Description = "Log out from the system")
    @Operation(summary = "Sign-out(logout)", description = "log-out from the system")
    @Audit(auditType = "Logout")
    public boolean logout(@HeaderParam("authenticationkey") String authenticationkey,
                          @Context SecurityContext securityContext) {

        return userservice.logOut(authenticationkey);

    }

}
