/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.rest;

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
import com.flexicore.interceptors.SecurityImposer;
import com.flexicore.interfaces.RESTService;
import com.flexicore.model.Job;
import com.flexicore.request.RegisterForJobUpdates;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/jobProcess")
@RequestScoped
@Component
@OperationsInside
@Protected
@Tag(name = "Upload")
@Tag(name = "Core")

public class JobRESTService implements RESTService {

	@Autowired
	private JobService jobService;

	/**
	 * retreives Job object
	 * 
	 * @param authenticationkey authentication key
	 * @param jobID
	 *            requested job id
	 * @param securityContext security context
	 * @return job object containing information
	 */
	@GET
	@Path("{jobID}")
	@Produces(MediaType.APPLICATION_JSON)
	@IOperation(access=Access.allow,Name="gets job",Description="gets a job by id")
	public Job getJob(@HeaderParam("authenticationkey") String authenticationkey, @PathParam("jobID") String jobID,@Context SecurityContext securityContext) {
		return jobService.checkJobStatus(jobID);
	}

	@POST
	@Path("test")
	@Produces(MediaType.APPLICATION_JSON)
	@IOperation(access=Access.allow,Name="gets job",Description="gets a job by id")
	public Job test(@HeaderParam("authenticationkey") String authenticationkey,@Context SecurityContext securityContext) {
		return jobService.test(securityContext);
	}


	@PUT
	@Path("registerForJobUpdates")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "registerForJobUpdates",description = "Register for job updates")
	public void registerForJobUpdates(@HeaderParam("authenticationkey") String authenticationkey,
								  RegisterForJobUpdates registerForJobUpdates,
								  @Context SecurityContext securityContext) {
		jobService.registerForJobUpdates(registerForJobUpdates,securityContext);
	}

	/**
	 * updates a Job property
	 *
	 * @param authenticationkey authentication key
	 * @param securityContext security context
	 * @param key key
	 * @param value value
	 * @param jobID requested job id
	 */
	@PUT
	@Path("updateJobProperty/{jobID}/{key}/{value}")
	@Produces(MediaType.APPLICATION_JSON)
	@IOperation(access=Access.allow,Name="update job",Description="update's job property")
	public void updateJobProperty(@HeaderParam("authenticationkey") String authenticationkey, @PathParam("jobID") String jobID,
								  @PathParam("key") String key,
								  @PathParam("value") String value,
								  @Context SecurityContext securityContext) {
		jobService.updateJobProperty(jobID,key,value);
	}

	/**
	 * updates a Job property
	 *
	 * @param authenticationkey authentication key
	 * @param securityContext security context
	 * @param phaseName phase name
	 * @param jobID requested job id
	 */
	@PUT
	@Path("updateJobPhase/{jobID}/{phaseName}")
	@Produces(MediaType.APPLICATION_JSON)
	@IOperation(access=Access.allow,Name="update job",Description="update's job property")
	public void updateJobPhase(@HeaderParam("authenticationkey") String authenticationkey, @PathParam("jobID") String jobID,
								  @PathParam("phaseName") String phaseName,
								  @Context SecurityContext securityContext) {
		jobService.updateJobPhase(jobID,phaseName);
	}

	/**
	 * stops a Job
	 *
	 * @param authenticationkey authentication key
	 * @param securityContext security context
	 * @param jobID requested job id
	 */
	@DELETE
	@Path("{jobID}")
	@Produces(MediaType.APPLICATION_JSON)
	@IOperation(access=Access.allow,Name="stop job",Description="stops job property")
	public void stopJob(@HeaderParam("authenticationkey") String authenticationkey, @PathParam("jobID") String jobID,
								  @Context SecurityContext securityContext) {
		jobService.cancel(jobID);
	}



}
