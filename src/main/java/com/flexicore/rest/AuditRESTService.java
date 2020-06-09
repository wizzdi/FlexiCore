/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.rest;

import com.flexicore.annotations.OperationsInside;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.interceptors.SecurityImposer;
import com.flexicore.interfaces.RESTService;
import com.flexicore.model.auditing.AuditingEvent;
import com.flexicore.request.AuditingFilter;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.AuditingService;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.enterprise.context.RequestScoped;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.flexicore.annotations.Protected;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

@Path("/audit")
@RequestScoped
@Component
@OperationsInside
@Protected
@Tag(name = "Audit")
@Tag(name = "Core")
@ExternalDocumentation(description = "Audit Services")
public class AuditRESTService implements RESTService {
	@Autowired
	private AuditingService service;


	@POST
	@Produces("application/json")
	@Operation(summary = "getAllAuditingEvents", description = "lists all auditingEvents")
	@Path("getAllAuditingEvents")
	public PaginationResponse<AuditingEvent> getAllAuditingEvents(
			@HeaderParam("authenticationKey") String authenticationKey,
			AuditingFilter auditingFilter,
			@Context SecurityContext securityContext) {
		service.validate(auditingFilter,securityContext);
		return service.getAllAuditingEvents(auditingFilter, securityContext);
	}



}
