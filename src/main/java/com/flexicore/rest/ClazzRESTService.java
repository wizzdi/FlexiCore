/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.flexicore.annotations.Protected;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.flexicore.annotations.IOperation;
import com.flexicore.annotations.IOperation.Access;
import com.flexicore.annotations.OperationsInside;
import com.flexicore.annotations.rest.*;
import com.flexicore.data.jsoncontainers.ClazzLinkContainer;
import com.flexicore.data.jsoncontainers.FieldContainer;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.interceptors.SecurityImposer;
import com.flexicore.interfaces.RESTService;
import com.flexicore.model.*;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.BaseclassService;
import com.flexicore.service.impl.BaselinkService;
import com.flexicore.service.impl.ClazzService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Component;

//documented ion the 28 of June 2016
@Path("/clazz")
@RequestScoped
@Component
@OperationsInside
@Protected
@Tag(name = "Core")
@Tag(name = "Clazz")
public class ClazzRESTService implements RESTService {

    //DONE: provide a service to get a list of clazzes.
    @Autowired
    private ClazzService clazzService;


    @Autowired
    private BaselinkService baselinkService;

    @Autowired
    private BaseclassService baseclassService;


    private Logger log = Logger.getLogger(getClass().getCanonicalName());


    @POST
    @Path("listAllClazz")
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "listAllClazz", Description = "returns all clazzes", relatedClazzes = {Clazz.class})
    @Operation(summary = "list all clazz", description = "returns a list of clazzes")
    public PaginationResponse<Clazz> listAllClazz(@HeaderParam("authenticationkey") String authenticationkey,
														FilteringInformationHolder filteringInformationHolder,
             @Context SecurityContext securityContext) {
        List<Clazz> list = baseclassService.getAllByKeyWordAndCategory(new QueryInformationHolder<>(filteringInformationHolder, Clazz.class, securityContext));
        long count=baseclassService.count(Clazz.class,filteringInformationHolder,securityContext);
        return new PaginationResponse<>(list,filteringInformationHolder,count);

    }


    @GET
    @Path("fields/{clazzName}")
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "getClazzFields", Description = "returns all fields of clazz", relatedClazzes = {Clazz.class})
    @Operation(summary = "Get Field Containers per class", description = "get a list of all FieldContainers per class, use it for the creation of a new instance and for updating one")
    public List<FieldContainer> getFields(@HeaderParam("authenticationkey") String authenticationkey,
                                          @Parameter(description = "The canonical name of the Class for which the list is to be obtained") @PathParam("clazzName") String clazzName, @Context SecurityContext securityContext) {
        Clazz c = clazzService.getclazz(clazzName);
        List<FieldContainer> fields = clazzService.getFields(c, securityContext);
        if (fields != null) {
            log.log(Level.INFO, "Fields returned: " + fields.size());
        } else {
            log.log(Level.INFO, "NULL Fields returned ");
        }
        return fields;

    }

    @GET
    @Path("/{clazzName}")
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "getClazzFromName", Description = "returns a Clazz instance for the supplied name", relatedClazzes = {Clazz.class})
    @Operation(summary = "Get a Clazz instance from a canonical name", description = "Return a concrete instance of type Clazz, every type in the system including those created by Plug-ins have a concrete instance of Clazz type describing it.")
    public Clazz getClazz(@HeaderParam("authenticationkey") String authenticationkey,
                          @Parameter(description = "The canonical class name for which the instance is required") @PathParam("clazzName") String clazzName, @Context SecurityContext securityContext) {

        return clazzService.getclazz(clazzName);

    }

    @GET
    @Path("associations/{clazzName}")
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "get Clazz Associated Clazzes", Description = "returns all classes associated with a given class, classes are associated if they have a ? extends Baselink between them", relatedClazzes = {Clazz.class})
    @Operation(summary = "Get a list of all associations ", description = "Returns a list of ClazzLinkContainer instances of the given link(!) canonical name")
    public List<ClazzLinkContainer> getAssociations(@HeaderParam("authenticationkey") String authenticationkey, @PathParam("clazzName") String clazzName, @Context SecurityContext securityContext) {
        Clazz c = clazzService.getclazz(clazzName);
        return clazzService.getAssociations(c, securityContext);

    }

    @GET
    @Path("operations/{clazzName}")
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "getClazzFields", Description = "returns related Operations", relatedClazzes = {com.flexicore.model.Operation.class})
    @Operation(summary = "Get all Operations", description = "Get a list of all Operations defined for the this Class, Some Operations have meaning with some Classes only")
    public List<com.flexicore.model.Operation> getAllOperations(@HeaderParam("authenticationkey") String authenticationkey,
                                                                @Parameter(description = "The canonical class name of the link required") @PathParam("clazzName") String clazzName, @Context SecurityContext securityContext) {
        Clazz c = clazzService.getclazz(clazzName);
        List<OperationToClazz> operations = baselinkService.findAllBySide(OperationToClazz.class, c, true, securityContext);
        List<com.flexicore.model.Operation> ops = new ArrayList<>();
        for (OperationToClazz operationToClazz : operations) {
            ops.add(operationToClazz.getLeftside());
        }
        addDefaultOperations(ops);

        return ops;

    }

    private void addDefaultOperations(List<com.flexicore.model.Operation> ops) {
        ops.add(baselinkService.findById(Baseclass.generateUUIDFromString(Write.class.getCanonicalName())));
        ops.add(baselinkService.findById(Baseclass.generateUUIDFromString(Read.class.getCanonicalName())));

        ops.add(baselinkService.findById(Baseclass.generateUUIDFromString(Update.class.getCanonicalName())));

        ops.add(baselinkService.findById(Baseclass.generateUUIDFromString(Delete.class.getCanonicalName())));
        ops.add(baselinkService.findById(Baseclass.generateUUIDFromString(All.class.getCanonicalName())));

    }


    public ClazzRESTService() {
        // TODO Auto-generated constructor stub
    }

}
