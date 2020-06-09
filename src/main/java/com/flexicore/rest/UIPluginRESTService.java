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
import com.flexicore.data.jsoncontainers.UIComponentsRegistrationContainer;
import com.flexicore.data.jsoncontainers.UIPluginContainer;
import com.flexicore.data.jsoncontainers.UIPluginCreationContainer;
import com.flexicore.interceptors.SecurityImposer;
import com.flexicore.interfaces.RESTService;
import com.flexicore.model.*;
import com.flexicore.model.ui.UIComponent;
import com.flexicore.model.ui.UIContainer;
import com.flexicore.model.ui.plugins.UIInterface;
import com.flexicore.model.ui.plugins.UIPlugin;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.BaselinkService;
import com.flexicore.service.impl.UIPluginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.enterprise.context.RequestScoped;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.flexicore.annotations.Protected;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("/uiPlugin")
@RequestScoped
@Component
@OperationsInside
@Protected
@Tag(name = "UIPlugin")
@Tag(name = "Core")
public class UIPluginRESTService implements RESTService {


    @Autowired
    private UIPluginService uiPluginService;

    @Autowired
    private BaselinkService baselinkService;


    //DONE: allow existing Category to be associated with a Clazz by name
    //DONE: provide a service for disconnecting a Clazz from Category.
    //DONE: provide a service to disconnect (disable) a Category from Baseclass.


  /*  @POST
    @Path("listUIPluginsByInterface/{interfaceId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)

    @IOperation(access = Access.allow, Name = "listUIPluginsByInterface", Description = "lists plugin by interface", relatedClazzes = {UIPlugin.class, UIInterface.class})
    @Operation(summary = "listUIPluginsByInterface", description = "lists plugin by interface")

    public List<UIPluginContainer> listUIPluginsByInterface(@HeaderParam("authenticationkey") String authenticationkey,
                                                   @HeaderParam("interfaceId") String interfaceId,
                                                   @HeaderParam("pagesize") @DefaultValue("-1") int pageSize,
                                                   @HeaderParam("currentpage") @DefaultValue("-1") int currentPage,
                                                   FilteringInformationHolder filteringInformationHolder,
                                                   @Context SecurityContext securityContext) {

        UIInterface uiInterface = baselinkService.getByIdOrNull(interfaceId, UIInterface.class, null, securityContext);
        if (uiInterface == null) {
            throw new BadRequestException("no ui interface with id " + interfaceId);
        }


        return uiPluginService.listUIPluginsByInterface(uiInterface, filteringInformationHolder, pageSize, currentPage, securityContext).parallelStream().map(f->new UIPluginContainer(f)).collect(Collectors.toList());

    }*/


    @POST
    @Path("listUIInterfaces")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)

    @IOperation(access = Access.allow, Name = "listUIInterfaces", Description = "lists ui interfaces", relatedClazzes = {UIInterface.class})
    @Operation(summary = "listUIInterfaces", description = "lists ui interfaces")

    public List<UIInterface> listUIInterfaces(@HeaderParam("authenticationkey") String authenticationkey,
                                              @HeaderParam("pagesize") @DefaultValue("-1") int pageSize,
                                              @HeaderParam("currentpage") @DefaultValue("-1") int currentPage,
                                              FilteringInformationHolder filteringInformationHolder,
                                              @Context SecurityContext securityContext) {


        return uiPluginService.listUIInterfaces(filteringInformationHolder, pageSize, currentPage, securityContext);

    }


    @POST
    @Path("registerUIPlugin")
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "registerUIPlugin", Description = "registers ui plugin", relatedClazzes = {UIPlugin.class})
    @Operation(summary = "registerUIPlugin", description = "registers ui plugin")

    public UIPluginContainer registerUIPlugin(@HeaderParam("authenticationkey") String authenticationkey,
                                              UIPluginCreationContainer uiPluginCreationContainer,
                                              @Context SecurityContext securityContext) {


        List<UIInterface> interfaces = new ArrayList<>();
        for (String id : uiPluginCreationContainer.getInterfacesIds()) {
            UIInterface uiInterface = baselinkService.getByIdOrNull(id, UIInterface.class, null, securityContext);
            if (uiInterface == null) {
                throw new BadRequestException("no ui interface with id " + id);
            }
            interfaces.add(uiInterface);
        }
        List<FileResource> fileResources= new ArrayList<>();
        for (String id : uiPluginCreationContainer.getFileResourceIds()) {
            FileResource fileResource = baselinkService.getByIdOrNull(id, FileResource.class, null, securityContext);
            fileResources.add(fileResource);

        }
        uiPluginCreationContainer.setFileResources(fileResources);
        uiPluginCreationContainer.setUiInterfaces(interfaces);

        return new UIPluginContainer(uiPluginService.registerUIPlugin(uiPluginCreationContainer, securityContext));

    }


    @POST
    @Path("createUIInterface/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "createUIInterface", Description = "creates UI interface", relatedClazzes = {UIInterface.class})
    @Operation(summary = "createUIInterface", description = "creates UI interface")

    public UIInterface createUIInterface(@HeaderParam("authenticationkey") String authenticationkey,
                                         @PathParam("name") String name,
                                         @Context SecurityContext securityContext) {
        return uiPluginService.createUIInterface(name,securityContext);
    }


    @POST
    @Path("registerAndGetAllowedUIComponents")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "registerAndGetAllowedUIComponents", Description = "registers components if not exists and returns allowed", relatedClazzes = {UIComponent.class, UIContainer.class})
    @Operation(summary = "registerAndGetAllowedUIComponents", description = "registers components if not exists and returns allowed")

    public List<UIComponent> registerAndGetAllowedUIComponents(@HeaderParam("authenticationkey") String authenticationkey,
                                          UIComponentsRegistrationContainer uiComponentsRegistrationContainer,
                                         @Context SecurityContext securityContext) {
        uiPluginService.validate(uiComponentsRegistrationContainer);
        return uiPluginService.registerAndGetAllowedUIComponents(uiComponentsRegistrationContainer.getComponentsToRegister(),securityContext);
    }


}
