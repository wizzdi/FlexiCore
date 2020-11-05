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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.RequestScoped;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.flexicore.annotations.Protected;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.interfaces.RESTService;
import com.flexicore.model.FilteringInformationHolder;
import com.flexicore.request.BaselinkCreate;
import com.flexicore.request.BaselinkFilter;
import com.flexicore.request.BaselinkMassCreate;
import com.flexicore.request.BaselinkUpdate;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import org.jboss.resteasy.spi.HttpResponseCodes;

import com.flexicore.annotations.IOperation;
import com.flexicore.annotations.IOperation.Access;
import com.flexicore.annotations.OperationsInside;

import com.flexicore.model.Baseclass;
import com.flexicore.model.Baselink;
import com.flexicore.model.RoleToUser;
import com.flexicore.model.User;
import com.flexicore.model.UserToBaseClass;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.BaseclassService;
import com.flexicore.service.impl.BaselinkService;

/**
 * @author avishayb
 *
 */
@Protected
@RequestScoped
@Component
@OperationsInside
@Path("/baselinks")
@Tag(name = "Core")
@Tag(name = "Baseclasses")
public class 	BaselinkRESTService implements RESTService {
	@Autowired
	private BaselinkService service;

	@Autowired
	private BaseclassService baseclassservice;

	private static final Logger log = LoggerFactory.getLogger(BaselinkRESTService.class);

	/**
	 * generic way of linking (M2M) of any ? extends Baseclass with another one.
	 * The link should be instantiated
	 * @param securityContext security context
	 * @param authenticationkey auethnetication key
	 * @param check check if existing before create
	 * @param leftId leftside id
	 * @param linkClazzName link type
	 * @param rightId right side id
	 * @param simpleValue simple value
	 * @param valueId value id
	 * @return created baselink
	 */
	@SuppressWarnings("unchecked")
	@POST
	@Path("/{leftId}/{linkClazzName}/{rightId}")
	@Produces(MediaType.APPLICATION_JSON)
	@IOperation(access = Access.allow, Name = "Create Base link", Description = "Generic link creation")
	@Operation(summary = "Create a link", description = "Creates a link between two instances in a generic way, provide values of the link")
	public Baselink createBaselink(@HeaderParam("authenticationkey") String authenticationkey,
			@Parameter(description = "an ID of existing instance, class of which must fit the required class for the supplied Link class") @PathParam("leftId") String leftId,
			@Parameter(description = "an ID of existing instance, class of which must fit the required class for the supplied Link class") @PathParam("rightId") String rightId,
			@Parameter(description = "The canonical name of the class of the link, for example: com.flexicore.model.MediaToBundle, in this case the left ID must be of Media class and the RightID must be of a bundle class") @PathParam("linkClazzName") String linkClazzName,
			@Parameter(description = "The ID of any instance inheriting from FC Baseclass, this effectively creates a triple link") @HeaderParam("value") @DefaultValue("") String valueId,
			@Parameter(description = "An explicit value of a string that can be part of the link") @HeaderParam("simpleValue") @DefaultValue("") String simpleValue,
			@Parameter(description = "If true, check if the link with these Left, Right and Complex Value values exists, if yes, then it will not be created") @HeaderParam("check") @DefaultValue("false") boolean check,
			@Context SecurityContext securityContext) {
		Baselink link;

		long time = System.currentTimeMillis();
		Class<? extends Baselink> clazz;
		try {
			clazz = (Class<? extends Baselink>) Class.forName(linkClazzName);
		} catch (ClassNotFoundException e) {
			throw new ClientErrorException("no class with name:" + linkClazzName, HttpResponseCodes.SC_BAD_REQUEST);
		}
		rightId= rightId.equals("null") ?null:rightId;
		Baseclass left = service.getByIdOrNull(leftId, Baseclass.class, null, securityContext);
		if(left==null){
			throw new BadRequestException("No Baseclass with id "+leftId);
		}
		Baseclass right = rightId!=null ?service.getByIdOrNull(rightId, Baseclass.class, null, securityContext):null;
		if(rightId!=null&&right==null){
			throw new BadRequestException("No Baseclass With id "+rightId);
		}
		Baseclass value = null;
		if (valueId != null && !valueId.isEmpty()) {
			value = service.getByIdOrNull(valueId, Baseclass.class, null, securityContext);
			if(value==null){
				throw new BadRequestException("No Baseclass with Id "+valueId);
			}
		}
		if (check) {
			link = service.linkEntities(left, right, clazz, value, simpleValue);
		} else {
			link = service.linkEntitiesNoCheck(left, right, value, simpleValue, clazz, securityContext);
		}
		log.info("baselink creation took: " + (System.currentTimeMillis() - time));
		return link;

	}

	@POST
	@Path("/massCreateBaselink")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "massCreateBaselink", description = "Creates connection between all the instnaces on the left and all those on the right, not creating existing ones")
	public List<Baselink> massCreateBaselink(@HeaderParam("authenticationkey") String authenticationkey,
								BaselinkMassCreate baselinkMassCreate,
								   @Context SecurityContext securityContext) {


		service.validate(baselinkMassCreate,securityContext);
		return service.massCreateBaselink(baselinkMassCreate,securityContext);

	}


	@POST
	@Path("/createBaselink")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Create a link", description = "Creates a link between two instances in a generic way, provide values of the link")
	public Baselink createBaselink(@HeaderParam("authenticationkey") String authenticationkey,
											 BaselinkCreate baselinkCreate,
											 @Context SecurityContext securityContext) {


		service.validate(baselinkCreate,securityContext);
		return service.createBaselink(baselinkCreate,securityContext);

	}

	@PUT
	@Path("/updateBaselink")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Create a link", description = "Creates a link between two instances in a generic way, provide values of the link")
	public Baselink updateBaselink(@HeaderParam("authenticationkey") String authenticationkey,
								   BaselinkUpdate baselinkUpdate,
								   @Context SecurityContext securityContext) {

		String id=baselinkUpdate.getId();
		Baselink baselink=id==null?null:service.getByIdOrNull(id,Baselink.class,null,securityContext);
		if(baselink==null){
			throw new BadRequestException("No Baselink with id "+id);
		}
		baselinkUpdate.setBaselink(baselink);
		service.validate(baselinkUpdate,securityContext);
		return service.updateBaselink(baselinkUpdate,securityContext);

	}






	@SuppressWarnings("unchecked")
	@DELETE
	@Path("/{leftId}/{linkClazzName}/{rightId}")
	@Produces(MediaType.APPLICATION_JSON)
	@IOperation(access = Access.allow, Name = "Removes Base link", Description = "Generic link deletion")
	@Operation(summary = "Remove a link", description = "Remove an existing link by the ID of its 'sides'")
	public void detachEntities(@HeaderParam("authenticationkey") String authenticationkey,
			@Parameter(description = "The ID of the left side of the link") @PathParam("leftId") String leftId,
			@Parameter(description = "The ID of the right side of the link") @PathParam("rightId") String rightId,
			@Parameter(description = "The canonical name of the class of the link, for example: com.flexicore.model.MediaToBundle, in this case the left ID must be of Media class and the RightID must be of a bundle class") @PathParam("linkClazzName") String linkClazzName,
			@Context SecurityContext securityContext) {
		Class<? extends Baselink> clazz;
		try {
			clazz = (Class<? extends Baselink>) Class.forName(linkClazzName);
		} catch (ClassNotFoundException e) {
			throw new ClientErrorException("no class with name:" + linkClazzName, HttpResponseCodes.SC_BAD_REQUEST);
		}
		Baseclass left = service.getByIdOrNull(leftId, Baseclass.class, null, securityContext);
		if(left==null){
			throw new BadRequestException("No Baseclass with id "+leftId);
		}
		rightId= rightId.equals("null") ?null:rightId;
		Baseclass right = rightId!=null?service.getByIdOrNull(rightId, Baseclass.class, null, securityContext):null;
		if(rightId!=null&&right==null){
			throw new BadRequestException("No Baseclass with id "+rightId);
		}
		Baselink link = service.findBySides(clazz, left, right);
		service.remove(link);

	}


	@PUT
	@Path("/linkUserToBaseclass/{left_id}/{right_id}/{operation_id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@IOperation(access = Access.deny, Name = "Link Role to User", Description = "Link a Role to a User, User can be linked to many Roles")
	public boolean linkBaseclassTouser(@HeaderParam("authenticationkey") String authenticationkey,
			@PathParam("left_id") String left, @PathParam("right_id") String right,
			@PathParam("operation_id") String operationId) {
		Baseclass base = service.findById(right);
		User user = service.findById(left);
		com.flexicore.model.Operation operation = service.findById(operationId);
		UserToBaseClass existing = service.findBySidesAndValue(user, base, operation, UserToBaseClass.class);
		if (existing == null) {
			UserToBaseClass link = service.linkEntities(user, base, UserToBaseClass.class);
			link.setValue(operation);
			link.setSimplevalue(IOperation.Access.allow.name());
			service.merge(link);

			return true;
		} else {
			if (existing.getSimplevalue().equals(IOperation.Access.deny.name())) {
				existing.setSimplevalue(IOperation.Access.allow.name());
				service.merge(existing);
				return true;
			} else {
				return false;
			}
		}

	}


	@SuppressWarnings("unchecked")
	@POST
	@Path("findLinks/{left}/{right}/{classname}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@IOperation(access = Access.allow, Name = "Search for m2m relationship", Description = "Generic link search by pair of objects")

	public <T extends Baselink> List<T> findLinks(@HeaderParam("authenticationkey") String authenticationkey,
								  @PathParam("left") String leftId, @PathParam("right") String rightId,
								  @PathParam("classname") String linkClazzName, @HeaderParam("value") @DefaultValue("") String valueId,
								  @HeaderParam("simpleValue") @DefaultValue("-1") String simpleValue,
									FilteringInformationHolder filter,
									@HeaderParam("pagesize")  @DefaultValue("-1") int pagesize,
									@HeaderParam("currentpage")  @DefaultValue("-1") int currentpage,

								  @Context SecurityContext securityContext) {
		log.info( "Finding sides: " + linkClazzName + " leftID: " + leftId + " rightID: " + rightId);
		if(filter==null){
			filter=new FilteringInformationHolder();
		}
		Class<T> clazz;
		try {
			clazz = (Class<T>) Class.forName(linkClazzName);
		} catch (ClassNotFoundException e) {
			throw new ClientErrorException("no class with name:" + linkClazzName, HttpResponseCodes.SC_BAD_REQUEST);
		}
		long start = System.currentTimeMillis();
		Baseclass leftside = service.getById(leftId, Baseclass.class, null, securityContext);
		log.info( "Time taken to find by id: " + (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();
		Baseclass rightside = service.getById(rightId, Baseclass.class, null, securityContext);
		log.info( "Time taken to find by id: " + (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();
		Baseclass value = null;
		if (!valueId.isEmpty()) {
			value = service.getById(valueId, Baseclass.class, null, securityContext);
		}
		if (simpleValue.equals("-1")) {
			simpleValue = null;
		}
		List<T> baselink = service.findAllBySidesAndValue(clazz,leftside,rightside,value,simpleValue,filter,pagesize,currentpage,securityContext);
		log.info( "Time taken to find sides: " + (System.currentTimeMillis() - start));
		return baselink;

	}




	@POST
	@Path("getAllBaselinks")
	@Consumes("application/json")
	@Produces("application/json")
	@IOperation(access = Access.allow, Name = "returns all baselinks", Description = "gets connected instances to the instance supplied (by ID) filtered by Link type")
	public PaginationResponse<Baselink> getAllBaselinks(@HeaderParam("authenticationkey") String authenticationkey,
														BaselinkFilter baselinkFilter,
														@Context SecurityContext securityContext) {
		service.validate(baselinkFilter,securityContext);
		return service.getAllBaselinks(baselinkFilter,securityContext);

	}


	@SuppressWarnings("unchecked")
	@POST
	@Path("findLinksValues/{left}/{right}/{classname}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@IOperation(access = Access.allow, Name = "Search for m2m relationship", Description = "Generic link search by pair of objects")

	public <T extends Baselink> List<Baseclass> findLinksValues(@HeaderParam("authenticationkey") String authenticationkey,
												  @PathParam("left") String leftId, @PathParam("right") String rightId,
												  @PathParam("classname") String linkClazzName, @HeaderParam("value") @DefaultValue("") String valueId,
												  @HeaderParam("simpleValue") @DefaultValue("-1") String simpleValue,
												  FilteringInformationHolder filter,
												  @HeaderParam("pagesize")  @DefaultValue("-1") int pagesize,
												  @HeaderParam("currentpage")  @DefaultValue("-1") int currentpage,

												  @Context SecurityContext securityContext) {
		log.info( "Finding sides: " + linkClazzName + " leftID: " + leftId + " rightID: " + rightId);
		Class<T> clazz;
		try {
			clazz = (Class<T>) Class.forName(linkClazzName);
		} catch (ClassNotFoundException e) {
			throw new ClientErrorException("no class with name:" + linkClazzName, HttpResponseCodes.SC_BAD_REQUEST);
		}
		long start = System.currentTimeMillis();
		Baseclass leftside = service.getById(leftId, Baseclass.class, null, securityContext);
		log.info( "Time taken to find by id: " + (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();
		Baseclass rightside = service.getById(rightId, Baseclass.class, null, securityContext);
		log.info( "Time taken to find by id: " + (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();
		Baseclass value = null;
		if (!valueId.isEmpty()) {
			value = service.getById(valueId, Baseclass.class, null, securityContext);
		}
		if (simpleValue.equals("-1")) {
			simpleValue = null;
		}
		List<Baseclass> baselink = service.getAllValues(clazz,leftside,rightside,value,simpleValue,filter,pagesize,currentpage,securityContext);
		log.info( "Time taken to find sides: " + (System.currentTimeMillis() - start));
		return baselink;

	}





	@SuppressWarnings("unchecked")
	@GET
	@Path("/{left}/{right}/{classname}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@IOperation(access = Access.allow, Name = "Search for m2m relationship", Description = "Generic link search by pair of objects")

	public Response findBySidesId(@HeaderParam("authenticationkey") String authenticationkey,
			@PathParam("left") String leftId, @PathParam("right") String rightId,
			@PathParam("classname") String linkClazzName, @HeaderParam("value") @DefaultValue("") String valueId,
			@HeaderParam("simpleValue") @DefaultValue("-1") String simpleValue,
			@Context SecurityContext securityContext) {
		log.info( "Finding sides: " + linkClazzName + " leftID: " + leftId + " rightID: " + rightId);
		Class<? extends Baselink> clazz;
		try {
			clazz = (Class<? extends Baselink>) Class.forName(linkClazzName);
		} catch (ClassNotFoundException e) {
			throw new ClientErrorException("no class with name:" + linkClazzName, HttpResponseCodes.SC_BAD_REQUEST);
		}
		long start = System.currentTimeMillis();
		Baseclass leftside = service.getById(leftId, Baseclass.class, null, securityContext);
		log.info( "Time taken to find by id: " + (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();
		Baseclass rightside = service.getById(rightId, Baseclass.class, null, securityContext);
		log.info( "Time taken to find by id: " + (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();
		Baseclass value = null;
		if (!valueId.isEmpty()) {
			value = service.getById(valueId, Baseclass.class, null, securityContext);
		}
		if (simpleValue.equals("-1")) {
			simpleValue = null;
		}
		Baselink baselink = service.findBySidesAndValue(leftside, rightside, value, simpleValue, clazz);
		log.info( "Time taken to find sides: " + (System.currentTimeMillis() - start));

		if (baselink == null) {
			return Response.status(Status.NOT_FOUND).build();
		}
		return Response.ok(baselink).build();
	}

}
