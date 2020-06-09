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
import com.flexicore.data.jsoncontainers.*;
import com.flexicore.exceptions.BadRequestCustomException;
import com.flexicore.interfaces.RESTService;
import com.flexicore.model.User;
import com.flexicore.service.impl.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.enterprise.context.RequestScoped;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/tokenBased")
@RequestScoped
@Component
@OperationsInside
@Tag(name = "tokenBased")

public class TokenBasedRESTService implements RESTService {

	@Autowired
	private UserService userService;



	@PUT
	@Path("resetPasswordFinish")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@IOperation(access=Access.allow,Name="reset password",Description="resets user's password using verification number")
	public ResetPasswordResponse resetPassword(
			ResetPasswordWithVerification resetPasswordWithVerification) {
		return userService.resetPasswordWithVerification(resetPasswordWithVerification).setMessage("Success");

	}


	@PUT
	@Path("verifyMailFinish")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@IOperation(access=Access.allow,Name="verifyMailFinish",Description="Verifies user's mail using token")
	public VerifyMailResponse verifyMailFinish(
			VerifyMail verifyMail) {
		return userService.verifyMail(verifyMail).setMessage("Success");

	}


}
