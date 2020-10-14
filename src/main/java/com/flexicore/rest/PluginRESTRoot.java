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

import com.flexicore.data.jsoncontainers.PluginType;
import com.flexicore.interfaces.RESTService;
import com.flexicore.model.ModuleManifest;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.PluginService;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.enterprise.context.RequestScoped;

import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.flexicore.annotations.Protected;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/plugins")
@RequestScoped
@Component
@OperationsInside
@Protected
 @Tag(name = "Core")
@Tag(name = "Plugins")

public class PluginRESTRoot implements RESTService {

    //TODO:get all implemented plugin interfaces types
    //TODO:get all loaded plugin services
    //TODO:REST to return logs
    //TODO:REST get plugin description
    @Autowired
    @Lazy
    private PluginManager pluginManager;

    public PluginRESTRoot() {
        // TODO Auto-generated constructor stub
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "listAllLoadedPlugins", Description = "lists all plugins")
    public List<ModuleManifest> listAllLoadedPlugins(@HeaderParam("authenticationkey") String authenticationkey, @Context SecurityContext securityContext) {
        List<ModuleManifest> moduleManifests=new ArrayList<>(com.flexicore.service.PluginService.externalModules);
        moduleManifests.addAll(
                pluginManager.getStartedPlugins().stream().map(f->new ModuleManifest(f.getPluginId(),f.getDescriptor().getVersion(),"","",null,null,PluginType.Service)).collect(Collectors.toList())
        );
        return moduleManifests;

    }

    @GET
    @Path("/modules")
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "listAllModules", Description = "lists all modules")
    public List<ModuleManifest> listAllModules(@HeaderParam("authenticationkey") String authenticationkey, @Context SecurityContext securityContext) {
        List<ModuleManifest> moduleManifests=new ArrayList<>(com.flexicore.service.PluginService.externalModules);
        moduleManifests.addAll(
                pluginManager.getStartedPlugins().stream().map(f->new ModuleManifest(f.getPluginId(),f.getDescriptor().getVersion(),"","",null,null,PluginType.Service)).collect(Collectors.toList())
        );
        return moduleManifests;


    }











}
