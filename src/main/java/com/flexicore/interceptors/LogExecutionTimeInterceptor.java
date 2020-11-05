package com.flexicore.interceptors;

import com.flexicore.annotations.LogExecutionTime;

import javax.annotation.Priority;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * Created by Asaf on 18/12/2016.
 */
@Component
@Aspect
public class LogExecutionTimeInterceptor implements Serializable{

   private static final Logger logger = LoggerFactory.getLogger(LogExecutionTimeInterceptor.class);


    @Around("execution(@com.flexicore.annotations.LogExecutionTime * *(..)) || within(@(@com.flexicore.annotations.LogExecutionTime *) *)")
    public Object aroundGetter(ProceedingJoinPoint invocationContext) throws Throwable {
         long start=System.currentTimeMillis();
         Object o=invocationContext.proceed();
        MethodSignature signature = (MethodSignature) invocationContext.getSignature();
        Method method = signature.getMethod();
         logger.info("Method: "+method.getName() +" took: "+(System.currentTimeMillis()-start) +" ms");
         return o;





    }
}
