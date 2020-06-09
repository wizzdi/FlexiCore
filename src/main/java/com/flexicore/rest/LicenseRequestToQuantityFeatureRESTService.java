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
import com.flexicore.model.licensing.LicenseRequestToQuantityFeature;
import com.flexicore.request.LicenseRequestToQuantityFeatureCreate;
import com.flexicore.request.LicenseRequestToQuantityFeatureFiltering;
import com.flexicore.request.LicenseRequestToQuantityFeatureUpdate;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.LicenseRequestToQuantityFeatureService;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.enterprise.context.RequestScoped;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.flexicore.annotations.Protected;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.logging.Logger;


@Path("/licenseRequestToQuantityFeatures")
@RequestScoped
@Component
@OperationsInside
@Protected
@Tag(name = "License")


public class LicenseRequestToQuantityFeatureRESTService implements RESTService {


    @Autowired
    private LicenseRequestToQuantityFeatureService licenseRequestToQuantityFeatureService;
   private Logger logger = Logger.getLogger(getClass().getCanonicalName());




   

    @POST
    @Path("/getAllLicenseRequestToQuantityFeatures")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "getAllLicenseRequestToQuantityFeatures", Description = "lists LicenseRequestToQuantityFeatures", relatedClazzes = {LicenseRequestToQuantityFeature.class},noOtherLicenseRequired = true)
    public PaginationResponse<LicenseRequestToQuantityFeature> getAllLicenseRequestToQuantityFeatures(@HeaderParam("authenticationkey") String authenticationkey
            , LicenseRequestToQuantityFeatureFiltering licenseRequestToQuantityFeatureFiltering, @Context SecurityContext securityContext) {
        licenseRequestToQuantityFeatureService.validate(licenseRequestToQuantityFeatureFiltering, securityContext);
        return licenseRequestToQuantityFeatureService.getAllLicenseRequestToQuantityFeatures(licenseRequestToQuantityFeatureFiltering, securityContext);

    }

  

    @POST
    @Path("/createLicenseRequestToQuantityFeature")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "Creates LicenseRequestToQuantityFeature", Description = "Creates LicenseRequestToQuantityFeature", relatedClazzes = {LicenseRequestToQuantityFeature.class},noOtherLicenseRequired = true)
    public LicenseRequestToQuantityFeature createLicenseRequestToQuantityFeature(@HeaderParam("authenticationkey") String authenticationkey
            , LicenseRequestToQuantityFeatureCreate licenseRequestToQuantityFeatureCreate, @Context SecurityContext securityContext) {
        licenseRequestToQuantityFeatureService.validate(licenseRequestToQuantityFeatureCreate, securityContext);
        return licenseRequestToQuantityFeatureService.createLicenseRequestToQuantityFeature(licenseRequestToQuantityFeatureCreate, securityContext);

    }

    @PUT
    @Path("/updateLicenseRequestToQuantityFeature")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "Updates LicenseRequestToQuantityFeature", Description = "Updates LicenseRequestToQuantityFeature", relatedClazzes = {LicenseRequestToQuantityFeature.class},noOtherLicenseRequired = true)
    public LicenseRequestToQuantityFeature updateLicenseRequestToQuantityFeature(@HeaderParam("authenticationkey") String authenticationkey
            , LicenseRequestToQuantityFeatureUpdate licenseRequestToQuantityFeatureUpdate, @Context SecurityContext securityContext) {
        String id=licenseRequestToQuantityFeatureUpdate.getId();
        LicenseRequestToQuantityFeature licenseRequestToQuantityFeature=id!=null?licenseRequestToQuantityFeatureService.getByIdOrNull(id,LicenseRequestToQuantityFeature.class,null,securityContext):null;
        if(licenseRequestToQuantityFeature==null){
            throw new BadRequestException("No LicenseRequestToQuantityFeature with id "+id);
        }
        licenseRequestToQuantityFeatureUpdate.setLicenseRequestToQuantityFeature(licenseRequestToQuantityFeature);
        licenseRequestToQuantityFeatureService.validate(licenseRequestToQuantityFeatureUpdate, securityContext);
        return licenseRequestToQuantityFeatureService.updateLicenseRequestToQuantityFeature(licenseRequestToQuantityFeatureUpdate, securityContext);

    }

}
