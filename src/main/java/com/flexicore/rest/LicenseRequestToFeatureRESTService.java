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
import com.flexicore.model.licensing.LicenseRequestToFeature;
import com.flexicore.request.LicenseRequestToFeatureCreate;
import com.flexicore.request.LicenseRequestToFeatureFiltering;
import com.flexicore.request.LicenseRequestToFeatureUpdate;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.LicenseRequestToFeatureService;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.enterprise.context.RequestScoped;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.flexicore.annotations.Protected;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.logging.Logger;


@Path("/licenseRequestToFeatures")
@RequestScoped
@Component
@OperationsInside
@Protected
@Tag(name = "License")


public class LicenseRequestToFeatureRESTService implements RESTService {


    @Autowired
    private LicenseRequestToFeatureService licenseRequestToFeatureService;
   private Logger logger = Logger.getLogger(getClass().getCanonicalName());




   

    @POST
    @Path("/getAllLicenseRequestToFeatures")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "getAllLicenseRequestToFeatures", Description = "lists LicenseRequestToFeatures", relatedClazzes = {LicenseRequestToFeature.class},noOtherLicenseRequired = true)
    public PaginationResponse<LicenseRequestToFeature> getAllLicenseRequestToFeatures(@HeaderParam("authenticationkey") String authenticationkey
            , LicenseRequestToFeatureFiltering licenseRequestToFeatureFiltering, @Context SecurityContext securityContext) {
        licenseRequestToFeatureService.validate(licenseRequestToFeatureFiltering, securityContext);
        return licenseRequestToFeatureService.getAllLicenseRequestToFeatures(licenseRequestToFeatureFiltering, securityContext);

    }

  

    @POST
    @Path("/createLicenseRequestToFeature")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "Creates LicenseRequestToFeature", Description = "Creates LicenseRequestToFeature", relatedClazzes = {LicenseRequestToFeature.class},noOtherLicenseRequired = true)
    public LicenseRequestToFeature createLicenseRequestToFeature(@HeaderParam("authenticationkey") String authenticationkey
            , LicenseRequestToFeatureCreate licenseRequestToFeatureCreate, @Context SecurityContext securityContext) {
        licenseRequestToFeatureService.validate(licenseRequestToFeatureCreate, securityContext);
        return licenseRequestToFeatureService.createLicenseRequestToFeature(licenseRequestToFeatureCreate, securityContext);

    }

    @PUT
    @Path("/updateLicenseRequestToFeature")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "Updates LicenseRequestToFeature", Description = "Updates LicenseRequestToFeature", relatedClazzes = {LicenseRequestToFeature.class},noOtherLicenseRequired = true)
    public LicenseRequestToFeature updateLicenseRequestToFeature(@HeaderParam("authenticationkey") String authenticationkey
            , LicenseRequestToFeatureUpdate licenseRequestToFeatureUpdate, @Context SecurityContext securityContext) {
        String id=licenseRequestToFeatureUpdate.getId();
        LicenseRequestToFeature licenseRequestToFeature=id!=null?licenseRequestToFeatureService.getByIdOrNull(id,LicenseRequestToFeature.class,null,securityContext):null;
        if(licenseRequestToFeature==null){
            throw new BadRequestException("No LicenseRequestToFeature with id "+id);
        }
        licenseRequestToFeatureUpdate.setLicenseRequestToFeature(licenseRequestToFeature);
        licenseRequestToFeatureService.validate(licenseRequestToFeatureUpdate, securityContext);
        return licenseRequestToFeatureService.updateLicenseRequestToFeature(licenseRequestToFeatureUpdate, securityContext);

    }

}
