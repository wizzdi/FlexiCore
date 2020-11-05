/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.service.impl;

import com.flexicore.data.SecurityLinkRepository;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.model.SecurityLink;
import com.flexicore.request.SecurityLinkFilter;
import com.flexicore.security.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@Primary
@Component
public class SecurityLinkService implements com.flexicore.service.SecurityLinkService {

	private static final Logger log= LoggerFactory.getLogger(SecurityLinkService.class);

	@Autowired
	private SecurityLinkRepository securityLinkrepository;



	@Override
	public void validate(SecurityLinkFilter securityLinkFilter, SecurityContext securityContext) {

	}


	@Override
	public PaginationResponse<SecurityLink> getAllSecurityLinks(SecurityLinkFilter securityLinkFilter, SecurityContext securityContext) {

		List<SecurityLink> list= listAllSecurityLinks(securityLinkFilter, securityContext);
		long count=securityLinkrepository.countAllSecurityLinks(securityLinkFilter,securityContext);
		return new PaginationResponse<>(list,securityLinkFilter,count);

	}

	@Override
	public List<SecurityLink> listAllSecurityLinks(SecurityLinkFilter securityLinkFilter, SecurityContext securityContext) {
		return securityLinkrepository.getAllSecurityLinks(securityLinkFilter,securityContext);
	}


}
