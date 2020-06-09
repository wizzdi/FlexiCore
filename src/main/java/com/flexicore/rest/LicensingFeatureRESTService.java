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
import com.flexicore.model.licensing.LicensingFeature;
import com.flexicore.request.LicensingFeatureCreate;
import com.flexicore.request.LicensingFeatureFiltering;
import com.flexicore.request.LicensingFeatureUpdate;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.LicensingFeatureService;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.enterprise.context.RequestScoped;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.flexicore.annotations.Protected;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.logging.Logger;


@Path("/licensingFeatures")
@RequestScoped
@Component
@OperationsInside
@Protected
@Tag(name = "License")


public class LicensingFeatureRESTService implements RESTService {


    @Autowired
    private LicensingFeatureService licensingFeatureService;
   private Logger logger = Logger.getLogger(getClass().getCanonicalName());




   

    @POST
    @Path("/getAllLicensingFeatures")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "getAllLicensingFeatures", Description = "lists LicensingFeatures", relatedClazzes = {LicensingFeature.class},noOtherLicenseRequired = true)
    public PaginationResponse<LicensingFeature> getAllLicensingFeatures(@HeaderParam("authenticationkey") String authenticationkey
            , LicensingFeatureFiltering licensingFeatureFiltering, @Context SecurityContext securityContext) {
        licensingFeatureService.validate(licensingFeatureFiltering, securityContext);
        return licensingFeatureService.getAllLicensingFeatures(licensingFeatureFiltering, securityContext);

    }


}
