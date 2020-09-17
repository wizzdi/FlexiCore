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
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.interfaces.RESTService;
import com.flexicore.model.Tenant;
import com.flexicore.model.TenantToUser;
import com.flexicore.model.User;
import com.flexicore.request.*;
import com.flexicore.security.NewUser;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.TenantService;
import com.flexicore.service.impl.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;


@Path("/tenantToUser")
@RequestScoped
@Component
@OperationsInside
@Protected
@Tag(name = "TenantToUser")
public class TenantToUserRESTService implements RESTService {


    

    @Autowired
    private UserService userService;


    

    @POST
    @Path("getAllTenantToUsers")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "getTenantToUsers", Description = "returns all tenantToUsers", relatedClazzes = {TenantToUser.class},noOtherLicenseRequired = true)
    public PaginationResponse<TenantToUser> getAllTenantToUsers(@HeaderParam("authenticationkey") String authenticationkey,
                                                                TenantToUserFilter filteringInformationHolder,
                                                                @Context SecurityContext securityContext) {

        return userService.listAllTenantToUsers(filteringInformationHolder, securityContext);


    }

    @POST
    @Path("createTenantToUser")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "createTenantToUser", Description = "creates tenantToUser", relatedClazzes = {TenantToUser.class, User.class,Tenant.class},noOtherLicenseRequired = true)
    public TenantToUser createTenantToUser(@HeaderParam("authenticationkey") String authenticationkey,
                               TenantToUserCreate tenantToUserCreate,
                               @Context SecurityContext securityContext) {
        userService.validate(tenantToUserCreate,securityContext);
        return userService.createTenantToUser(tenantToUserCreate, securityContext);


    }

    @PUT
    @Path("updateTenantToUser")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "updateTenantToUser", Description = "update tenantToUser", relatedClazzes = {TenantToUser.class, User.class,Tenant.class})
    public TenantToUser updateTenantToUser(@HeaderParam("authenticationkey") String authenticationkey,
                               TenantToUserUpdate tenantToUserUpdate,
                               @Context SecurityContext securityContext) {
        String id= tenantToUserUpdate.getId();
        TenantToUser tenantToUser=id==null?null:userService.getByIdOrNull(id,TenantToUser.class,null,securityContext);
        if(tenantToUser==null){
            throw new BadRequestException("No TenantToUser with id "+id);
        }
        tenantToUserUpdate.setTenantToUser(tenantToUser);
        userService.validate(tenantToUserUpdate,securityContext);
        return userService.updateTenantToUser(tenantToUserUpdate, securityContext);


    }

}
