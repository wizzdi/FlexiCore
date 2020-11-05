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
import com.flexicore.interfaces.RESTService;
import com.flexicore.service.impl.FileResourceService;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/downloadUnsecure")
@RequestScoped
@Component
@OperationsInside

public class DownloadUnsecureRESTService implements RESTService {

	@Autowired
	FileResourceService fileResourceService;

	private static final Logger logger = LoggerFactory.getLogger(DownloadUnsecureRESTService.class);

    @GET
    @Path("{id}")
    @IOperation(access = Access.allow, Name = "downloadFile", Description = "downloads file by its fileResource ID")
    public Response download(@Parameter(description = "id of the FileResource Object to Download")
            @HeaderParam("offset") @DefaultValue("0") long offset,
            @HeaderParam("size") @DefaultValue("0") long size,
            @PathParam("id") String id, @Context HttpServletRequest req) {
        return fileResourceService.download(offset, size, id, req.getRemoteAddr(), null);

    }
}
