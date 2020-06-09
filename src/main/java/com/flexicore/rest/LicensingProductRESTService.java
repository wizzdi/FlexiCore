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
import com.flexicore.model.licensing.LicensingProduct;
import com.flexicore.request.LicensingProductFiltering;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.LicensingProductService;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.enterprise.context.RequestScoped;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.flexicore.annotations.Protected;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.logging.Logger;


@Path("/licensingProducts")
@RequestScoped
@Component
@OperationsInside
@Protected
@Tag(name = "License")


public class LicensingProductRESTService implements RESTService {


    @Autowired
    private LicensingProductService licensingProductService;
   private Logger logger = Logger.getLogger(getClass().getCanonicalName());




   

    @POST
    @Path("/getAllLicensingProducts")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @IOperation(access = Access.allow, Name = "getAllLicensingProducts", Description = "lists LicensingProducts", relatedClazzes = {LicensingProduct.class},noOtherLicenseRequired = true)
    public PaginationResponse<LicensingProduct> getAllLicensingProducts(@HeaderParam("authenticationkey") String authenticationkey
            , LicensingProductFiltering licensingProductFiltering, @Context SecurityContext securityContext) {
        licensingProductService.validate(licensingProductFiltering, securityContext);
        return licensingProductService.getAllLicensingProducts(licensingProductFiltering, securityContext);

    }


}
