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
import com.flexicore.annotations.Protected;
import com.flexicore.data.jsoncontainers.UIComponentsRegistrationContainer;
import com.flexicore.interfaces.RESTService;
import com.flexicore.model.ui.UIComponent;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.BaselinkService;
import com.flexicore.service.impl.UIComponentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/uiPlugin")
@RequestScoped
@Component
@OperationsInside
@Protected
@Tag(name = "UIPlugin")
@Tag(name = "Core")
public class UIComponentRESTService implements RESTService {


    @Autowired
    private UIComponentService uiPluginService;

    @Autowired
    private BaselinkService baselinkService;


    @POST
    @Path("registerAndGetAllowedUIComponents")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "registerAndGetAllowedUIComponents", Description = "registers components if not exists and returns allowed", relatedClazzes = {UIComponent.class})
    @Operation(summary = "registerAndGetAllowedUIComponents", description = "registers components if not exists and returns allowed")

    public List<UIComponent> registerAndGetAllowedUIComponents(@HeaderParam("authenticationkey") String authenticationkey,
                                          UIComponentsRegistrationContainer uiComponentsRegistrationContainer,
                                         @Context SecurityContext securityContext) {
        uiPluginService.validate(uiComponentsRegistrationContainer);
        return uiPluginService.registerAndGetAllowedUIComponents(uiComponentsRegistrationContainer.getComponentsToRegister(),securityContext);
    }


}
