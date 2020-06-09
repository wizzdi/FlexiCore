/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
/**
 *
 */
package com.flexicore.rest;

import com.flexicore.annotations.IOperation;
import com.flexicore.annotations.IOperation.Access;
import com.flexicore.annotations.OperationsInside;
import com.flexicore.annotations.ProtectedREST;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.interceptors.SecurityImposer;
import com.flexicore.interfaces.RESTService;
import com.flexicore.model.RoleToUser;
import com.flexicore.request.RoleToUserCreate;
import com.flexicore.request.RoleToUserFilter;
import com.flexicore.request.RoleToUserUpdate;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.RoleToUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.enterprise.context.RequestScoped;
import javax.interceptor.Interceptors;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@ProtectedREST
@OperationsInside
@Component
@Path("/roleToUsers")
@Tag(name = "Core")
@Tag(name = "RoleToUsers")

public class RoleToUserRESTService implements RESTService {


    @Autowired
    private RoleToUserService roleToUserService;


    @POST
    @Path("getAllRoleToUsers")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @IOperation(access=Access.allow,Name="get all roleToUsers",Description="get all RoleToUsers",relatedClazzes = {RoleToUser.class})
    public PaginationResponse<RoleToUser> getAllRoleToUsers(@HeaderParam("authenticationkey") String authenticationkey,
                                                            RoleToUserFilter roleToUserFilter,
                                                            @Context SecurityContext securityContext) {
        roleToUserService.validate(roleToUserFilter,securityContext);
        return roleToUserService.getAllRoleToUsers(roleToUserFilter,securityContext);
    }

    @POST
    @Path("createRoleToUser")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @IOperation(access=Access.allow,Name="get all roleToUsers",Description="get all RoleToUsers",relatedClazzes = {RoleToUser.class})
    public RoleToUser createRoleToUser(@HeaderParam("authenticationkey") String authenticationkey,
                                       RoleToUserCreate roleToUserCreate,
                                       @Context SecurityContext securityContext) {
        roleToUserService.validate(roleToUserCreate,securityContext);
        return roleToUserService.createRoleToUser(roleToUserCreate,securityContext);
    }

    @PUT
    @Path("updateRoleToUser")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @IOperation(access=Access.allow,Name="get all roleToUsers",Description="get all RoleToUsers",relatedClazzes = {RoleToUser.class})
    public RoleToUser updateRoleToUser(@HeaderParam("authenticationkey") String authenticationkey,
                                       RoleToUserUpdate roleToUserCreate,
                                       @Context SecurityContext securityContext) {
        RoleToUser roleToUser=roleToUserService.getByIdOrNull(roleToUserCreate.getId(),RoleToUser.class,null,securityContext);
        if(roleToUser==null){
            throw new BadRequestException("No RoleToUser with id "+roleToUserCreate.getId());
        }
        roleToUserCreate.setRoleToUser(roleToUser);
        return roleToUserService.updateRoleToUser(roleToUserCreate,securityContext);
    }




}
