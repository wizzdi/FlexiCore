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
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.interceptors.SecurityImposer;
import com.flexicore.interfaces.RESTService;
import com.flexicore.model.licensing.LicenseRequest;
import com.flexicore.request.LicenseRequestCreate;
import com.flexicore.request.LicenseRequestFiltering;
import com.flexicore.request.LicenseRequestUpdate;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.LicenseRequestService;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.enterprise.context.RequestScoped;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.flexicore.annotations.Protected;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.logging.Logger;


@Path("/licenseRequests")
@RequestScoped
@Component
@OperationsInside
@Protected
@Tag(name = "License")


public class LicenseRequestRESTService implements RESTService {


    @Autowired
    private LicenseRequestService licenseRequestService;
   private Logger logger = Logger.getLogger(getClass().getCanonicalName());




   

    @POST
    @Path("/getAllLicenseRequests")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "getAllLicenseRequests", Description = "lists LicenseRequests", relatedClazzes = {LicenseRequest.class},noOtherLicenseRequired = true)
    public PaginationResponse<LicenseRequest> getAllLicenseRequests(@HeaderParam("authenticationkey") String authenticationkey
            , LicenseRequestFiltering licenseRequestFiltering, @Context SecurityContext securityContext) {
        licenseRequestService.validate(licenseRequestFiltering, securityContext);
        return licenseRequestService.getAllLicenseRequests(licenseRequestFiltering, securityContext);

    }

  

    @POST
    @Path("/createLicenseRequest")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "Creates LicenseRequest", Description = "Creates LicenseRequest", relatedClazzes = {LicenseRequest.class},noOtherLicenseRequired = true)
    public LicenseRequest createLicenseRequest(@HeaderParam("authenticationkey") String authenticationkey
            , LicenseRequestCreate licenseRequestCreate, @Context SecurityContext securityContext) {
        licenseRequestService.validate(licenseRequestCreate, securityContext);
        licenseRequestService.validateCreate(licenseRequestCreate);
        return licenseRequestService.createLicenseRequest(licenseRequestCreate, securityContext);

    }

    @PUT
    @Path("/updateLicenseRequest")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "Updates LicenseRequest", Description = "Updates LicenseRequest", relatedClazzes = {LicenseRequest.class},noOtherLicenseRequired = true)
    public LicenseRequest updateLicenseRequest(@HeaderParam("authenticationkey") String authenticationkey
            , LicenseRequestUpdate licenseRequestUpdate, @Context SecurityContext securityContext) {
        String id=licenseRequestUpdate.getId();
        LicenseRequest licenseRequest=id!=null?licenseRequestService.getByIdOrNull(id,LicenseRequest.class,null,securityContext):null;
        if(licenseRequest==null){
            throw new BadRequestException("No License request with id "+id);
        }
        licenseRequestUpdate.setLicenseRequest(licenseRequest);
        licenseRequestService.validate(licenseRequestUpdate, securityContext);
        return licenseRequestService.updateLicenseRequest(licenseRequestUpdate, securityContext);

    }

}
