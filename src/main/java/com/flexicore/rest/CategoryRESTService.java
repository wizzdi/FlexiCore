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
import com.flexicore.interceptors.SecurityImposer;
import com.flexicore.interfaces.RESTService;
import com.flexicore.model.Baseclass;
import com.flexicore.model.Category;
import com.flexicore.model.Clazz;
import com.flexicore.model.User;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.CategoryService;
import com.flexicore.service.impl.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jboss.resteasy.spi.HttpResponseCodes;

import javax.enterprise.context.RequestScoped;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.flexicore.annotations.Protected;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/category")
@RequestScoped
@Component
@OperationsInside
@Protected
@Tag(name = "Categories")
@Tag(name = "Core")
public class CategoryRESTService implements RESTService {
	@Autowired
	private CategoryService categoryService;
	 
	@Autowired
	private UserService userService;
	
	
	
	//DONE: allow existing Category to be associated with a Clazz by name
	//DONE: provide a service for disconnecting a Clazz from Category.
	//DONE: provide a service to disconnect (disable) a Category from Baseclass.
	
	
	
		@POST
		@Produces(MediaType.APPLICATION_JSON)
		@IOperation(access = Access.allow,  Name = "CreateCategory", Description = "Create a new Caetgroy",relatedClazzes = {Category.class})
		@Operation(summary = "Create a Category" )

		public Category createCategory(@HeaderParam("authenticationkey") String authenticationkey,
			@Parameter(required=true,description="Inside a user interface categories are displayed by thier name") @HeaderParam("categoryName")String category,
									   @HeaderParam("checkForExisting") @DefaultValue("true") boolean checkForExisting,
									   @Context SecurityContext securityContext) {
			User user=securityContext.getUser();

			return categoryService.createCategory(category,checkForExisting, securityContext);
			
		}
		
		@PUT
		@Path("{baseId}")
		@Produces(MediaType.APPLICATION_JSON)
		@IOperation(access = Access.allow,  Name = "listAllCategories", Description = "lists all categories",relatedClazzes = {Category.class,Baseclass.class})
		@Operation(summary = "Connect to Category", description = "Connect a Category to an instance of any entity in the system")
		public boolean connectCategory(@HeaderParam("authenticationkey") String authenticationkey,
				@Parameter(description="The ID of an existing entity in the system, entity must inherit from FC Baseclass") @PathParam("baseId") String baseId,
				@Parameter(description="The ID of an existing Category")@HeaderParam("catId") String catId,@HeaderParam("checkForExisting") @DefaultValue("true") boolean checkForExisting,@Context SecurityContext securityContext){
		
			User user=userService.getUser(authenticationkey);
			return categoryService.connectCategory(baseId, catId, securityContext,checkForExisting);
			
		}
		
		@DELETE
		@Path("{baseId}")
		@Produces(MediaType.APPLICATION_JSON)
		@IOperation(access = Access.allow,  Name = "disconnectCategory", Description = "delete category",relatedClazzes = {Category.class,Baseclass.class})
		@Operation(summary = "Disconnect from Category", description = "Disconnect a Category from an instance of a connected(to this Category) entity in the system")
		public boolean disconnectCategory(@HeaderParam("authenticationkey") String authenticationkey,
				@Parameter(description="The ID of an existing entity in the system, entity must inherit from FC Baseclass and connected to the specified Category")@PathParam("baseId") String baseId,
				@Parameter(description="The ID of an existing Category") @HeaderParam("catId") String catId,@Context SecurityContext securityContext) {
			User user=userService.getUser(authenticationkey);
			return categoryService.disconnectCategory(baseId, catId, user);
			
		}
/**
 * associate a Baseclass with an existing Category, the server checks if the Category can be associated with the Clazz of the supplied Baseclass instance.		
 * @param authenticationkey
 * @throws ClassNotFoundException
 * @throws InstantiationException
 * @throws IllegalAccessException
 */
	
			@SuppressWarnings("unchecked")
			@PUT
			@Path("enable/{class_name}")
			@Produces(MediaType.APPLICATION_JSON)
			@Consumes(MediaType.APPLICATION_JSON)
			@IOperation(access = Access.allow,  Name = "enableCategory", Description = "enable category",relatedClazzes = {Category.class,Clazz.class})
			@Operation(summary = "Enable Category on Class", description = "Before a Category can be connected to an INSTANCE of a class it must be enabled to the class, thus a list of categories can be easily filtered by the context of the class at hand, the CategoryID to be enabled is passed as the entity in the Post request(!)")
			public void enableCategory(@HeaderParam("authenticationkey") String authenticationkey,
				@Parameter(description="the cannonical name of a Class of an Entity in the system, such Class must extend Baseclass, for Example: 'com.flexicore.model.User' ") @PathParam("class_name") String className,
				String catId,
				@Context SecurityContext securityContext) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
				User user=securityContext.getUser();
				Class<? extends Baseclass> c;
				try{
					c=(Class<? extends Baseclass>) Class.forName(className);
				}
				catch(ClassNotFoundException e){
					throw new ClientErrorException(HttpResponseCodes.SC_BAD_REQUEST);
				}
				Category cat=categoryService.get(catId);
				 categoryService.enableCategory(cat, c, securityContext);

			}
			
			@PUT
			@Path("{class_name}/disable")
			@Produces(MediaType.APPLICATION_JSON)
			@IOperation(access = Access.allow,  Name = "disableCategory", Description = "disable Category",relatedClazzes = {Category.class,Clazz.class})
			@Operation(summary = "Disable Category on Class", description = "Disable a previously disabled Category, the CategoryID to be enabled is passed as the entity in the Post request(!)")
		
			public void disableCategory(@HeaderParam("authenticationkey") String authenticationkey,
					@Parameter(description="the cannonical name of a Class of an Entity in the system, such Class must extend Baseclass, for Example: 'com.flexicore.model.User' ")@PathParam("class_name") String className,String catId,@Context SecurityContext securityContext) {
				Category cat=categoryService.get(catId);
				 categoryService.disableCategory(cat, className,securityContext);
				
			}
			/** 
			 * get all Categories of the supplied name
			 * @param authenticationkey
			 * @param name
			 * @return
             */
			@GET
			@Path("/byname/{name}")
			@Produces(MediaType.APPLICATION_JSON)
			@IOperation(access = Access.allow,  Name = "listAllCategoriesByName", Description = "lists all categories By name",relatedClazzes = {Category.class})
			@Operation(summary = "Get a list of Categories  by name", description = "Get a list of all Categories whose name starts with the supplied string")
		
			public List<Category> getCategoryByName(@HeaderParam("authenticationkey") String authenticationkey,
				@Parameter(description="A string that must match the left characters of a retrieved Category name") @PathParam("name") String name,@Context SecurityContext securityContext) {
				name="%"+name+"%";
				return categoryService.getByName(name,securityContext);
				
			}

	@GET
	@Path("/getAll")
	@Produces(MediaType.APPLICATION_JSON)
	@IOperation(access = Access.allow,  Name = "getAllCategories", Description = "lists all categories",relatedClazzes = {Category.class})
	@Operation(summary = "Get a list of Categories  by name", description = "Get a list of all Categories whose name starts with the supplied string")

	public List<Category> getAllCategories(@HeaderParam("authenticationkey") String authenticationkey,
										   @HeaderParam("pagesize") @DefaultValue("-1") Integer pagesize, @HeaderParam("currentpage") @DefaultValue("-1") Integer currentpage,
										   @Context SecurityContext securityContext) {
		return categoryService.getAllCategories(pagesize,currentpage,securityContext);

	}


			
			/** 
			 * get all Categories of the supplied name
			 * @param authenticationkey
			 * @return
             */
			@SuppressWarnings("unchecked")
			@GET
			@Path("{class_name}")
			@Produces(MediaType.APPLICATION_JSON)
			@IOperation(access = Access.allow,  Name = "getCategory", Description = "lists all categories by class",relatedClazzes = {Category.class})
			@Operation(summary = "List Categories  by Class", description = "Get a list of all Categories that can be used (previously enabled) by a Class")
		
			public List<Category> getCategory(@HeaderParam("authenticationkey") String authenticationkey,
				@Parameter(description="the canonical name of a Class of an Entity in the system, such Class must extend Baseclass, for Example: 'com.flexicore.model.media.Media' will retrieve only Categories that can be used with a Media Object(Instance)") @PathParam("class_name")String className,
				@Context SecurityContext securityContext) {
				Class<? extends Baseclass> c;
				try{
					c=(Class<? extends Baseclass>) Class.forName(className);
				}
				catch(ClassNotFoundException e){
					throw new ClientErrorException(HttpResponseCodes.SC_BAD_REQUEST);
				}
				if(c==null){
					throw new ClientErrorException("could not find class named: "+className, Response.Status.BAD_REQUEST);
				}
				return categoryService.getAllowedCategories(c, null, securityContext);
				
			}
			
			
			@GET
			@Path("/connected/{baseId}")
			@Produces(MediaType.APPLICATION_JSON)
			@IOperation(access = Access.allow,  Name = "getAllCategoriesConnected", Description = "get all categories connected",relatedClazzes = {Category.class,Baseclass.class})
			@Operation(summary = "List Categories  by Instance", description = "Get a list of all Categories that are connected to an instance")
		
			public List<Baseclass> getAllCategoriesConnected(@HeaderParam("authenticationkey") String authenticationkey
				,@Parameter(description="The ID of an existing entity in the system, entity must inherit from FC Baseclass")@PathParam("baseId") String baseId,@Context SecurityContext securityContext) {
				return categoryService.getAll(baseId, securityContext);
				
			}
		
		

	public CategoryRESTService() {
		// TODO Auto-generated constructor stub
	}

}
