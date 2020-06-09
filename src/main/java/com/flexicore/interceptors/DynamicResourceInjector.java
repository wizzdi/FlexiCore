/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.interceptors;

import com.flexicore.enums.Restriction;
import com.flexicore.interfaces.Plugin;
import com.flexicore.model.PluginRequirement;
import com.flexicore.service.impl.PluginService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * uses Aspect Oriented Programming (through JavaEE support) to enforce security
 * Access granularity is specified in a separate UML diagram
 *
 * @author Avishay Ben Natan
 */


@Aspect
@Component
public class DynamicResourceInjector implements Serializable {


    /**
     *
     */
    private static final long serialVersionUID = 1L;
    @Autowired
    PluginService pluginService;

    @Around("execution(@com.flexicore.annotations.DynamicREST * *(..)) || within(@(@com.flexicore.annotations.DynamicREST *) *)")
    public <T extends Plugin> Object transformReturn(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        MethodInvocationProceedingJoinPoint methodInvocationProceedingJoinPoint= (MethodInvocationProceedingJoinPoint) proceedingJoinPoint;
        Class<?> c = proceedingJoinPoint.getTarget().getClass();
        T toClean = null;
        if (Plugin.class.isAssignableFrom(c)) {
            HashMap<String, PluginRequirement> reqs = new HashMap<>();
            Object[] parameters = proceedingJoinPoint.getArgs();
            if (parameters.length > 1) {
                Object o1 = parameters[1];
                if (o1 instanceof String) {
                    reqs = stringToRequriments((String) o1);
                }
            }


            try {
                Class<T> pluginClass = (Class<T>) c;
                T o = pluginService.instansiateREST(pluginClass, reqs);
                Field invocF=methodInvocationProceedingJoinPoint.getClass().getDeclaredField("methodInvocation");
                invocF.setAccessible(true);
                ReflectiveMethodInvocation invoc=(ReflectiveMethodInvocation)invocF.get(methodInvocationProceedingJoinPoint);
                Field target=ReflectiveMethodInvocation.class.getDeclaredField("target");
                target.setAccessible(true);
                target.set(invoc,o);
                toClean = o;
            } catch (Exception e) {
                throw new ClientErrorException("Cannot satisfy plugin requirements", Response.Status.BAD_REQUEST, e);
            }
        }
        Object o = proceedingJoinPoint.proceed();
        if (toClean != null) {
            pluginService.cleanUpInstance(toClean);
        }
        return o;


    }

    private HashMap<String, PluginRequirement> stringToRequriments(String versionString) {
        String[] clazzes = versionString.split(";");
        PluginRequirement req;
        HashMap<String, PluginRequirement> reqs = new HashMap<>();
        for (String string : clazzes) {
            try {
                String[] reqForClass = string.split(":");
                String name = reqForClass[0];
                String integer = reqForClass[1];
                int version = Integer.parseInt(integer);
                req = new PluginRequirement(name, version, Restriction.Equals);
                reqs.put(name, req);
            } catch (Exception ignored) {

            }
        }
        return reqs;
    }

}
