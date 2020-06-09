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

import java.util.List;

import javax.enterprise.context.RequestScoped;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.flexicore.annotations.Protected;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.interfaces.RESTService;
import com.flexicore.model.FilteringInformationHolder;
import com.flexicore.model.Tenant;
import com.flexicore.model.User;
import com.flexicore.request.RoleCreate;
import com.flexicore.request.RoleFilter;
import com.flexicore.request.RoleUpdate;
import com.flexicore.service.impl.RoleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jboss.resteasy.spi.HttpResponseCodes;

import com.flexicore.annotations.IOperation;
import com.flexicore.annotations.IOperation.Access;
import com.flexicore.annotations.OperationsInside;
import com.flexicore.interceptors.SecurityImposer;
import com.flexicore.model.QueryInformationHolder;
import com.flexicore.model.Role;
import com.flexicore.security.SecurityContext;

@Protected
@OperationsInside
@Component
@Path("/roles")
@Tag(name = "Core")
@Tag(name = "Roles")

public class RoleRESTService implements RESTService {


	@Autowired
	private RoleService roleService;

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@IOperation(access = Access.allow,  Name = "Create New Role", Description = "Create new role in the system",relatedClazzes = {Tenant.class})

	public Role CreateRole(@HeaderParam("authenticationkey") String authenticationkey, String roleName,@Context SecurityContext securityContext) {
		Role role;
			try {
				role=roleService.createRole(roleName,securityContext);
			} catch (Exception e) {
				throw new ServerErrorException(HttpResponseCodes.SC_INTERNAL_SERVER_ERROR,e);
			}
			
		return role;
	}


	@GET
	@Path("{id}")
	@Produces("application/json")
	public Role findById(@HeaderParam("authenticationkey") String authenticationkey,
							 @PathParam("id") final String id,@Context SecurityContext securityContext) {

		Role role = roleService.findById(id,securityContext);

		if (role == null) {
			throw new ClientErrorException("now role with id: "+id, Response.Status.BAD_REQUEST);
		}
		return role;
	}


	@POST
	@Path("list")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@IOperation(access=Access.allow,Name="list all roles",Description="lists all Roles",relatedClazzes = {Role.class})
	public List<Role> listAllRoles(@HeaderParam("authenticationkey") String authenticationkey,
								   FilteringInformationHolder filteringInformationHolder,
								   @HeaderParam("pagesize") @DefaultValue(value="-1") Integer pagesize,
								   @HeaderParam("currentpage") @DefaultValue(value="-1")Integer currentpage,
								   @Context SecurityContext securityContext) {
		QueryInformationHolder<Role> queryInformationHolder = new QueryInformationHolder<>(filteringInformationHolder,pagesize,currentpage,Role.class,securityContext);
        return roleService.getAllFiltered(queryInformationHolder);
	}

	@POST
	@Path("getAllRoles")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@IOperation(access=Access.allow,Name="get all roles",Description="get all Roles",relatedClazzes = {Role.class})
	public PaginationResponse<Role> getAllRoles(@HeaderParam("authenticationkey") String authenticationkey,
												RoleFilter roleFilter,
												@Context SecurityContext securityContext) {
		roleService.validate(roleFilter,securityContext);
		return roleService.getAllRoles(roleFilter,securityContext);
	}

	@POST
	@Path("createRole")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@IOperation(access=Access.allow,Name="get all roles",Description="get all Roles",relatedClazzes = {Role.class})
	public Role createRole(@HeaderParam("authenticationkey") String authenticationkey,
												RoleCreate roleCreate,
												@Context SecurityContext securityContext) {
		roleService.validate(roleCreate,securityContext);
		return roleService.createRole(roleCreate,securityContext);
	}

	@PUT
	@Path("updateRole")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@IOperation(access=Access.allow,Name="get all roles",Description="get all Roles",relatedClazzes = {Role.class})
	public Role updateRole(@HeaderParam("authenticationkey") String authenticationkey,
						   RoleUpdate roleCreate,
						   @Context SecurityContext securityContext) {
		Role role=roleService.getByIdOrNull(roleCreate.getId(),Role.class,null,securityContext);
		if(role==null){
			throw new BadRequestException("No Role with id "+roleCreate.getId());
		}
		roleCreate.setRole(role);
		return roleService.updateRole(roleCreate,securityContext);
	}



	@GET
	@Path("userRoles/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@IOperation(access=Access.allow,Name="listUserRoles",Description="lists all user Roles",relatedClazzes = {Role.class,User.class})
	public List<Role> listAllUserRoles(@HeaderParam("authenticationkey") String authenticationkey,@PathParam("id") String id,@Context SecurityContext securityContext) {
		User user=roleService.getById(id,User.class,null,securityContext);
		QueryInformationHolder<Role> queryInformationHolder = new QueryInformationHolder<>(Role.class,securityContext);
        return roleService.getAllUserRoles(queryInformationHolder,user);
	}




}
