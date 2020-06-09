/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.rest;

import com.flexicore.annotations.OperationsInside;
import com.flexicore.data.jsoncontainers.CreatePermissionGroupLinkRequest;
import com.flexicore.data.jsoncontainers.CreatePermissionGroupRequest;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.interfaces.RESTService;
import com.flexicore.interceptors.SecurityImposer;
import com.flexicore.model.Baseclass;
import com.flexicore.model.PermissionGroup;
import com.flexicore.model.PermissionGroupToBaseclass;
import com.flexicore.request.PermissionGroupCopy;
import com.flexicore.request.PermissionGroupsFilter;
import com.flexicore.request.UpdatePermissionGroup;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.PermissionGroupService;
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
import java.util.Set;
import java.util.stream.Collectors;

@Path("/permissionGroup")
@RequestScoped
@Component
@OperationsInside
@Protected
@Tag(name = "PermissionGroup")
@Tag(name = "Core")
public class PermissionGroupRESTService implements RESTService {
    @Autowired
    private PermissionGroupService permissionGroupService;


    @POST
    @Path("createPermissionGroup")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "createPermissionGroup", description = "creates Permission Group")

    public PermissionGroup createPermissionGroup(@HeaderParam("authenticationkey") String authenticationkey,
                                                 CreatePermissionGroupRequest createPermissionGroupRequest,
                                                 @Context SecurityContext securityContext) {

        return permissionGroupService.createPermissionGroup(createPermissionGroupRequest, securityContext);

    }

    @POST
    @Path("copyPermissionGroup")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "copyPermissionGroup", description = "copies Permission Group")

    public PermissionGroup copyPermissionGroup(@HeaderParam("authenticationkey") String authenticationkey,
                                                 PermissionGroupCopy permissionGroupCopy,
                                                 @Context SecurityContext securityContext) {
        permissionGroupService.validate(permissionGroupCopy,securityContext);

        return permissionGroupService.copyPermissionGroup(permissionGroupCopy, securityContext);

    }

    @POST
    @Path("updatePermissionGroup")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "updatePermissionGroup", description = "Updates Permission Group")

    public PermissionGroup updatePermissionGroup(@HeaderParam("authenticationkey") String authenticationkey,
                                                 UpdatePermissionGroup updatePermissionGroup,
                                                 @Context SecurityContext securityContext) {
        PermissionGroup permissionGroup=permissionGroupService.getByIdOrNull(updatePermissionGroup.getId(),PermissionGroup.class,null,securityContext);
        if(permissionGroup==null){
            throw new BadRequestException("No Permission Group "+updatePermissionGroup.getId());
        }
        updatePermissionGroup.setPermissionGroup(permissionGroup);

        return permissionGroupService.updatePermissionGroup(updatePermissionGroup, securityContext);

    }

    @PUT
    @Path("connectPermissionGroupToBaseclass")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "connectPermissionGroupToBaseclass", description = "Connects Permission Group To Baseclass ")
    public List<PermissionGroupToBaseclass> connectPermissionGroupToBaseclass(@HeaderParam("authenticationkey") String authenticationkey,
                                                                              CreatePermissionGroupLinkRequest createPermissionGroupLinkRequest
            , @Context SecurityContext securityContext) {


        Set<String> baseclassIds = createPermissionGroupLinkRequest.getBaseclassIds();
        List<Baseclass> baseclass = !baseclassIds.isEmpty() ? permissionGroupService.listByIds(Baseclass.class, baseclassIds, securityContext) : new ArrayList<>();
        baseclassIds.removeAll(baseclass.parallelStream().map(f -> f.getId()).collect(Collectors.toSet()));
        if (!baseclassIds.isEmpty()) {
            throw new BadRequestException("No Baseclass With ids " + baseclassIds.parallelStream().collect(Collectors.joining(",")));
        }
        createPermissionGroupLinkRequest.setBaseclasses(baseclass);

        Set<String> groupsIds = createPermissionGroupLinkRequest.getGroupsIds();
        List<PermissionGroup> permissionGroups = !groupsIds.isEmpty() ? permissionGroupService.listByIds(PermissionGroup.class, groupsIds, securityContext) : new ArrayList<>();
        groupsIds.removeAll(permissionGroups.parallelStream().map(f -> f.getId()).collect(Collectors.toSet()));
        if (!groupsIds.isEmpty()) {
            throw new BadRequestException("No PermissionGroups With ids " + groupsIds.parallelStream().collect(Collectors.joining(",")));
        }
        createPermissionGroupLinkRequest.setPermissionGroups(permissionGroups);
        return permissionGroupService.connectPermissionGroupsToBaseclasses(createPermissionGroupLinkRequest, securityContext);

    }


    @POST
    @Path("getAllPermissionGroups")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "getAllPermissionGroups", description = "lists permission groups ")
    public PaginationResponse<PermissionGroup> getAllPermissionGroups(@HeaderParam("authenticationkey") String authenticationkey,
                                                                    PermissionGroupsFilter filteringInformationHolder,
                                                                    @Context SecurityContext securityContext) {

        return permissionGroupService.getAllPermissionGroups(filteringInformationHolder,securityContext);

    }

}
