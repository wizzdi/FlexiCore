/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.interceptors;

import com.flexicore.annotations.*;
import com.flexicore.data.jsoncontainers.OperationInfo;
import com.flexicore.model.Operation;
import com.flexicore.model.Tenant;
import com.flexicore.model.User;
import com.flexicore.model.auditing.AuditingJob;
import com.flexicore.model.auditing.DefaultAuditingTypes;
import com.flexicore.model.licensing.LicensingFeature;
import com.flexicore.request.LicensingFeatureFiltering;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.AuditingService;
import com.flexicore.service.impl.LicenseRequestService;
import com.flexicore.service.impl.LicensingFeatureService;
import com.flexicore.service.impl.SecurityService;
import io.jsonwebtoken.JwtException;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * uses Aspect Oriented Programming (through JavaEE support) to enforce security 
 * Access granularity is specified in a separate UML diagram
 * 
 * @author Avishay Ben Natan
 *
 */



@Aspect
@Component
public class SecurityImposer  {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Autowired
	private SecurityService securityService;
	private Logger logger = Logger.getLogger(getClass().getCanonicalName());
	@Autowired
	private LicenseRequestService licenseRequestService;
	@Autowired
	private LicensingFeatureService licensingFeatureService;
	@Autowired
	private AuditingService auditingService;

	@Around("execution(@com.flexicore.annotations.Protected * *(..)) || within(@(@com.flexicore.annotations.Protected *) *)|| within(@com.flexicore.annotations.Protected *)")
	public Object transformReturn(ProceedingJoinPoint joinPoint) throws Throwable {
		long start=System.currentTimeMillis();
		Session websocketSession=null;
		try {
			MethodSignature signature = (MethodSignature) joinPoint.getSignature();
			Method method = signature.getMethod();
			Object[] parameters=joinPoint.getArgs();
			String methodName = method.getName();
			logger.info("Method is: " + methodName + " , on Thread " + Thread.currentThread().getName());
			String authenticationkey = (String) parameters[0];
			String tenantApi = null;
			websocketSession=getWebsocketSession(parameters);


			if (authenticationkey != null && !authenticationkey.isEmpty()) {
				if (authenticationkey.contains("|")) {
					String[] l = authenticationkey.split("\\|");
					authenticationkey = l[0];
					tenantApi = l[1];
				}
				OperationInfo operationInfo = securityService.getIOperation(method);

				SecurityContext securityContext = securityService.getSecurityContext(authenticationkey, tenantApi, operationInfo.getOperationId());
				if (securityContext == null) {
					return deny(websocketSession);
				}
				User user = securityContext.getUser();
				List<Tenant> tenants = securityContext.getTenants();
				Operation operation = securityContext.getOperation();

				List<HasFeature> features = new ArrayList<>();
				OperationsInside operationsInside = method.getDeclaringClass().getAnnotation(OperationsInside.class);
				if (operationsInside != null) {
					features.addAll(Arrays.asList(operationsInside.features()));
				}
				if(SecurityService.getFlexiCoreFeature()!=null){
					features.add(SecurityService.getFlexiCoreFeature());
				}
				if (operationInfo.getiOperation() != null && user != null && tenants != null) {
					if(operationInfo.getiOperation().noOtherLicenseRequired()){
						features.clear();
					}
					features.addAll(Arrays.asList(operationInfo.getiOperation().features()));
					Set<String> canonicalNames=features.parallelStream().map(HasFeature::canonicalName).collect(Collectors.toSet());
					List<LicensingFeature> licensingFeatures = canonicalNames.isEmpty()?new ArrayList<>():licensingFeatureService.listAllLicensingFeatures(new LicensingFeatureFiltering().setCanonicalNames(canonicalNames),null);

					for (LicensingFeature feature : licensingFeatures) {
						if (!licenseRequestService.isFeatureLicensed(user, tenants, feature)) {
							return denyLicense(feature,websocketSession);
						}
					}


					if (securityService.checkIfAllowed(user, tenants, operation, operationInfo.getiOperation().access())) {
						return procceed(securityContext, joinPoint,methodName,parameters, start);
					} else {
						return deny(websocketSession);
					}

				} else {
					// TODO: policy if no operation available
					return deny(websocketSession);
				}

			} else {
				return deny(websocketSession);


			}
		}
		catch (JwtException e){
			logger.log(Level.SEVERE,"security check failed with error",e);
			return deny(websocketSession);
		}
		
	}

	private Session getWebsocketSession(Object[] parameters) {
		return parameters!=null?Stream.of(parameters).filter(f->f instanceof Session).map(f->(Session)f).findAny().orElse(null):null;
	}


	/**
	 *
	 * @param securityContext
	 * @param context
	 * @return
	 * @throws Exception
	 */
	private Object procceed(SecurityContext securityContext,ProceedingJoinPoint proceedingJoinPoint,String methodName,Object[] parameters,long start) throws Throwable {
		Object param=parameters[parameters.length-1];
		if(param instanceof Session){
			Session session= (Session) param;
			session.getUserProperties().put("securityContext",securityContext);
		}
		else{
			parameters[parameters.length - 1] = securityContext;
		}
		Object o= proceedingJoinPoint.proceed(parameters);
		long timeTaken = System.currentTimeMillis() - start;

		if(securityContext.getOperation().isAuditable()){
			auditingService.addAuditingJob(new AuditingJob(securityContext,null,o,timeTaken,Date.from(Instant.now()),DefaultAuditingTypes.REST.name()));
		}
		logger.info("request to "+methodName +" took: "+ timeTaken +"ms");
		return o;


	}
	



	
	private Object deny(Session websocketSession){
		String reason = "user is not authorized for this resource";
		closeWSIfNecessary(websocketSession, reason);
		throw new NotAuthorizedException(reason);
	}

	private void closeWSIfNecessary(Session websocketSession, String reason) {
		if(websocketSession!=null && websocketSession.isOpen()){
			try{
				String id = websocketSession.getId();

				websocketSession.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT,reason));
				logger.warning("Closed WS "+ id +" for being unauthorized");
			}
			catch (Exception e){
				logger.log(Level.SEVERE,"failed closing WS",e);
			}
		}
	}

	private Object denyLicense(LicensingFeature feature,Session websocketSession){
		String s= "invalid Feature License for Feature: "+feature.getCanonicalName();
		closeWSIfNecessary(websocketSession,s);

		throw new ForbiddenException(s);
	}
}
