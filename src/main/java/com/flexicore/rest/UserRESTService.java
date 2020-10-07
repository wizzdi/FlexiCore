/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.rest;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.flexicore.annotations.Protected;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.flexicore.annotations.IOperation;
import com.flexicore.annotations.IOperation.Access;
import com.flexicore.annotations.OperationsInside;
import com.flexicore.data.jsoncontainers.*;
import com.flexicore.interceptors.SecurityImposer;
import com.flexicore.interfaces.RESTService;
import com.flexicore.model.*;
import com.flexicore.request.*;
import com.flexicore.response.ImpersonateResponse;
import com.flexicore.response.UserProfile;
import com.flexicore.security.NewUser;
import com.flexicore.security.RunningUser;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


@Path("/users")
@RequestScoped
@Component
@OperationsInside
@Protected
@Tag(name = "Core")
@Tag(name = "Users")

public class UserRESTService implements RESTService {


    @Autowired
    private UserService userService;
   private Logger logger = Logger.getLogger(getClass().getCanonicalName());


    /**
     * Retrieves a list of all users
     * @deprecated replace with {@link #getAllUsers(String, UserFiltering, SecurityContext)}
     * @param authenticationkey authentication key
     * @param securityContext securiy context
     * @param getAllOfType filtering
     * @return {@code List<User>} at current page by page size
     */
    @Deprecated
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "listUsers", Description = "gets all users", relatedClazzes = {User.class})
    public List<User> listAllUsers(@HeaderParam("authenticationkey") String authenticationkey,
                                 FilteringInformationHolder getAllOfType
            , @Context SecurityContext securityContext) {
        QueryInformationHolder<User> queryInformationHolder = new QueryInformationHolder<>(getAllOfType,  User.class, securityContext);
        long start = System.currentTimeMillis();
        List<User> users = userService.getAllFiltered(queryInformationHolder);
        logger.log(Level.INFO, "got page at " + (System.currentTimeMillis() - start) + " millisecs");
        return users;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("impersonate")
    @Operation(description = "Impersonates", summary = "impersonates user at tenants")
    public ImpersonateResponse impersonate(@HeaderParam("authenticationkey") String authenticationkey,
                                           ImpersonateRequest impersonateRequest
            , @Context SecurityContext securityContext) {
        userService.validate(impersonateRequest, securityContext);
        return userService.impersonate(impersonateRequest,securityContext);
    }


    /**
     * Retrieves user by id
     * @param authenticationkey authentication key
     * @param securityContext security context
     * @param id requested {@code User} id
     * @return requested {@code User}
     */
    @GET
    @Path("/byId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.deny, Name = "Lookup User by ID", Description = "Find User by UUID", relatedClazzes = {User.class})
    public User lookupUserById(@HeaderParam("authenticationkey") String authenticationkey, @PathParam("id") String id, @Context SecurityContext securityContext) {
        User user = userService.findById(id);
        if (user == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return user;
    }

    /**
     * @param authenticationkey authentication key
     * @param Email requested {@code User} email
     * @param securityContext security context
     * @return requested {@code User}
     */
    @GET
    @Path("/byEmail/{email}")
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "Lookup User by Email", Description = "Find User by Email", relatedClazzes = {User.class})
    public User lookupUserByEmail(@HeaderParam("authenticationkey") String authenticationkey, @PathParam("email") String Email, @Context SecurityContext securityContext) {
        User user = userService.getUserByMail(Email, securityContext);
        if (user == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return user;
    }

    /**
     * Creates a new user, seems that the method is detected with no path parameter
     * returns a running user anyway, we need the user object
     *
     * @param authenticationkey authentication key
     * @param loginuponsuccess  true if do login after creation
     * @param newuser to create
     * @param <T> type of user to create
     * @param securityContext security ccontext
     * @return logged in user created
     */
    @POST
    @Path("/new")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "Create New User", Description = "Create new user in the system", relatedClazzes = {User.class})
    public <T extends User> RunningUser createUser(@HeaderParam("authenticationkey") String authenticationkey,
                                                   @HeaderParam("loginuponsuccess") boolean loginuponsuccess
            , NewUser<T> newuser, @Context SecurityContext securityContext) {
        userService.validateAndpopulateNewUser(newuser);

        Response.ResponseBuilder builder = null;

        try {
            RunningUser runninguser;
            if ((runninguser = userService.register(newuser, loginuponsuccess, securityContext)) != null) {
                if (runninguser.getLoggedin()) {
                    authenticationkey = runninguser.getAuthenticationkey().getKey();
                }

            }
            return runninguser;
        } catch (ConstraintViolationException ce) {
            // Handle bean validation issues
            throw new BadRequestException(ce);
        } catch (ValidationException e) {
            throw new BadRequestException("email taken", e);
        } catch (Exception e) {
            throw new InternalServerErrorException(e);
        }
    }

    @POST
    @Path("/getAllUsers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "getAllUsers", Description = "lists Users", relatedClazzes = {User.class})
    public PaginationResponse<User> getAllUsers(@HeaderParam("authenticationkey") String authenticationkey
            , UserFiltering userFiltering, @Context SecurityContext securityContext) {
        userService.validate(userFiltering, securityContext);
        return userService.getAllUsers(userFiltering, securityContext);

    }

    @POST
    @Path("/getUserProfile")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "getUserProfile", Description = "returns Users profile", relatedClazzes = {User.class})
    public UserProfile getUserProfile(@HeaderParam("authenticationkey") String authenticationkey
            , UserProfileRequest userProfileRequest, @Context SecurityContext securityContext) {
        return userService.getUserProfile(userProfileRequest, securityContext);

    }


    @POST
    @Path("/createUser")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "Creates User", Description = "Creates User", relatedClazzes = {User.class})
    public User createUser(@HeaderParam("authenticationkey") String authenticationkey
            , UserCreate userCreate, @Context SecurityContext securityContext) {
        userService.validateUserForCreate(userCreate, securityContext);
        return userService.createUser(userCreate, securityContext);

    }

    @PUT
    @Path("/updateUser")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "Updates User", Description = "Updates User", relatedClazzes = {User.class})
    public User updateUser(@HeaderParam("authenticationkey") String authenticationkey
            , UserUpdate userUpdate, @Context SecurityContext securityContext) {
        userService.validateUserUpdate(userUpdate, securityContext);
        return userService.updateUser(userUpdate, securityContext);

    }


    @POST
    @Path("/resetUserPassword")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "Reset User Password", Description = "Reset User password", relatedClazzes = {User.class})
    public ResetPasswordResponse resetUserPassword(@HeaderParam("authenticationkey") String authenticationkey,
                                                   ResetUserPasswordRequest resetUserPasswordRequest,
                                                   @Context SecurityContext securityContext) {
        User user = userService.getUserByMail(resetUserPasswordRequest.getEmail(), securityContext);
        if (user == null) {
            throw new BadRequestException("no user with mail " + resetUserPasswordRequest.getEmail());
        }
        resetUserPasswordRequest.setUser(user);

        return userService.resetUserPassword(resetUserPasswordRequest, securityContext);
    }



    @PUT
    @Path("/attachTennat")
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "attachTenant", Description = "Attach User to another tenant", relatedClazzes = {User.class, Tenant.class})
    public boolean attachTenant(@HeaderParam("authenticationkey") String authenticationkey, String apiKey, @Context SecurityContext context) {
        return userService.attachTenant(authenticationkey, apiKey, context);
    }

    @PUT
    @Path("/connectToTenant")
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "connectToTenant", Description = "connect user from tenant", relatedClazzes = {User.class, Tenant.class})
    public boolean connectToTenant(@HeaderParam("authenticationkey") String authenticationkey, String TenantId, @Context SecurityContext context) {
        return false;//userService.connectToTenant(context.getUser(), TenantId);
    }


    @PUT
    @Path("/addToRole/{roleId}/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "addUserToRole", Description = "connect user to role", relatedClazzes = {User.class, Role.class})
    public boolean addUserToRole(@HeaderParam("authenticationkey") String authenticationkey, @PathParam("roleId") String roleId, @PathParam("userId") String userId, @Context SecurityContext context) {
        return  false;//userService.addUserToRole(userId, roleId);
    }


    @POST
    @Path("/batchCreate")
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "addUserToRole", Description = "connect user to role", relatedClazzes = {User.class})
    public int multipleCreate(@HeaderParam("authenticationkey") String authenticationkey, @HeaderParam("number") @DefaultValue("0") int number, @Context SecurityContext securityContext) {
        return userService.multipleCreate(number, securityContext);
    }


}
