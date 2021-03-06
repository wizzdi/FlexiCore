/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.interceptors;

import com.flexicore.annotations.OperationsInside;
import com.flexicore.data.jsoncontainers.OperationInfo;
import com.flexicore.model.Operation;
import com.flexicore.model.Tenant;
import com.flexicore.model.User;
import com.flexicore.request.RecoverTotpRequest;
import com.flexicore.request.TotpAuthenticationRequest;
import com.flexicore.rest.TotpRESTService;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.SecurityService;
import io.jsonwebtoken.JwtException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import javax.ws.rs.NotAuthorizedException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

/**
 * uses Aspect Oriented Programming (through JavaEE support) to enforce security
 * Access granularity is specified in a separate UML diagram
 *
 * @author Avishay Ben Natan
 */


@Aspect
@Component
@Order(0)
public class SecurityImposer {


    /**
     *
     */
    private static final long serialVersionUID = 1L;
    @Autowired
    private SecurityService securityService;
    private static final Logger logger = LoggerFactory.getLogger(SecurityImposer.class);
    private static Method totpAuthenticationMethod;
    private static Method totpRecoveryMethod;

    @Around("execution(@com.flexicore.annotations.Protected * *(..)) || within(@(@com.flexicore.annotations.Protected *) *)|| within(@com.flexicore.annotations.Protected *)")
    public Object transformReturn(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Session websocketSession = null;
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Object[] parameters = joinPoint.getArgs();
            String methodName = method.getName();
            logger.info("Method is: " + methodName + " , on Thread " + Thread.currentThread().getName());
            String authenticationkey = (String) parameters[0];
            websocketSession = getWebsocketSession(parameters);


            if (authenticationkey != null && !authenticationkey.isEmpty()) {
                OperationInfo operationInfo = securityService.getIOperation(method);

                SecurityContext securityContext = securityService.getSecurityContext(authenticationkey, operationInfo.getOperationId());
                if (securityContext == null) {
                    return deny(websocketSession);
                }
                User user = securityContext.getUser();
                List<Tenant> tenants = securityContext.getTenants();
                Operation operation = securityContext.getOperation();


                OperationsInside operationsInside = method.getDeclaringClass().getAnnotation(OperationsInside.class);
                if (operationInfo.getiOperation() == null) {
                    logger.error("could not find io operation annotation on method: " + methodName);
                    return deny(websocketSession);
                }
                if (user == null) {
                    logger.error("could not determine user");
                    return deny(websocketSession);

                }
                if (tenants == null) {
                    logger.error("could not determine tenants");
                    return deny(websocketSession);
                }

                if (operation == null) {
                    logger.error("could not determine operation for method " + methodName);
                    return deny(websocketSession);
                }
                if (!isTotpAuthentication(method)&&user.isTotpEnabled() && !securityContext.isTotpVerified()) {
                    return deny(websocketSession);
                }

                if (securityService.checkIfAllowed(user, tenants, operation, operationInfo.getiOperation().access())) {
                    Object procceed = procceed(securityContext, joinPoint, methodName, parameters, start);
                    return procceed;
                } else {
                    return deny(websocketSession);
                }

            } else {
                return deny(websocketSession);


            }
        } catch (JwtException e) {
            logger.error("security check failed with error", e);
            return deny(websocketSession);
        }

    }

    private boolean isTotpAuthentication(Method method) {
        Method totpAuthenticationMethod= getTotpAuthenticationMethod();
        Method totpRecoveryMethod= getTotpRecoveryMethod();

        return method.equals(totpAuthenticationMethod)||method.equals(totpRecoveryMethod);
    }

    private Method getTotpAuthenticationMethod() {
        if(totpAuthenticationMethod==null){
            try {
                totpAuthenticationMethod = TotpRESTService.class.getMethod("authenticateTotp", String.class, TotpAuthenticationRequest.class, SecurityContext.class);
            }
            catch (Exception e){
                logger.error("could not find totp authenticationMethod");
            }
        }
        return totpAuthenticationMethod;
    }
    private Method getTotpRecoveryMethod() {
        if(totpRecoveryMethod==null){
            try {
                totpRecoveryMethod = TotpRESTService.class.getMethod("recoverTotp", String.class, RecoverTotpRequest.class, SecurityContext.class);
            }
            catch (Exception e){
                logger.error("could not find totp authenticationMethod");
            }
        }
        return totpRecoveryMethod;
    }

    private Session getWebsocketSession(Object[] parameters) {
        return parameters != null ? Stream.of(parameters).filter(f -> f instanceof Session).map(f -> (Session) f).findAny().orElse(null) : null;
    }


    private Object procceed(SecurityContext securityContext, ProceedingJoinPoint proceedingJoinPoint, String methodName, Object[] parameters, long start) throws Throwable {
        Object param = parameters[parameters.length - 1];
        if (param instanceof Session) {
            Session session = (Session) param;
            session.getUserProperties().put("securityContext", securityContext);
        } else {
            parameters[parameters.length - 1] = securityContext;
        }
        Object o = proceedingJoinPoint.proceed(parameters);
        long timeTaken = System.currentTimeMillis() - start;

        logger.info("request to " + methodName + " took: " + timeTaken + "ms");
        return o;


    }


    private Object deny(Session websocketSession) {
        String reason = "user is not authorized for this resource";
        closeWSIfNecessary(websocketSession, reason);
        throw new NotAuthorizedException(reason);
    }

    private void closeWSIfNecessary(Session websocketSession, String reason) {
        if (websocketSession != null && websocketSession.isOpen()) {
            try {
                String id = websocketSession.getId();

                websocketSession.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, reason));
                logger.warn("Closed WS " + id + " for being unauthorized");
            } catch (Exception e) {
                logger.error("failed closing WS", e);
            }
        }
    }

}
