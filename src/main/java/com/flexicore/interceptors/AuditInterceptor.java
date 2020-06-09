package com.flexicore.interceptors;

import com.flexicore.annotations.Audit;
import com.flexicore.data.jsoncontainers.OperationInfo;
import com.flexicore.interfaces.SecurityContextHolder;
import com.flexicore.model.Operation;
import com.flexicore.model.auditing.AuditingJob;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.OperationService;
import com.flexicore.service.impl.SecurityService;

import javax.annotation.Priority;
import org.springframework.beans.factory.annotation.Autowired;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.io.Serializable;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Created by Asaf on 18/12/2016.
 */
@Audit
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class AuditInterceptor implements Serializable {

   private Logger logger = Logger.getLogger(getClass().getCanonicalName());

   /*
    @Autowired
    private Event<AuditingJob> auditingJobEvent;*/

    @Autowired
    private SecurityService securityService;

    @Autowired
    private OperationService operationService;


    @AroundInvoke
    public Object audit(InvocationContext invocationContext) throws Exception {
        long start = System.currentTimeMillis();
        Date dateOccured = new Date();


        Audit audit = invocationContext.getMethod().getAnnotation(Audit.class);
        SecurityContext securityContext = getSecurityContext(invocationContext.getParameters());
        Object o = invocationContext.proceed();
        long timeTaken = System.currentTimeMillis() - start;
        OperationInfo operationInfo = securityService.getIOperation(invocationContext.getMethod());
        Operation operation=operationInfo!=null?operationService.getByIdOrNull(operationInfo.getOperationId(), Operation.class,null,null):null;
        if(operation!=null&&operation.isAuditable()){
            securityContext=securityContext!=null?securityContext:new SecurityContext().setOperation(operation);
            AuditingJob auditingJob = new AuditingJob()
                    .setInvocationContext(invocationContext)
                    .setAuditingType(audit!=null?audit.auditType():null)
                    .setDateOccured(dateOccured)
                    .setSecurityContext(securityContext)
                    .setTimeTaken(timeTaken)
                    .setResponse(o);
            //auditingJobEvent.fireAsync(auditingJob);
            logger.info("Method: " + invocationContext.getMethod().getName() + " took: " + (System.currentTimeMillis() - start) + " ms, and was sent to be audited");
        }

        return o;


    }

    private SecurityContext getSecurityContext(Object[] parameters) {
        if (parameters != null) {
            for (Object parameter : parameters) {
                if (parameter instanceof SecurityContext) {
                    return (SecurityContext) parameter;
                }
                if (parameter instanceof SecurityContextHolder) {
                    SecurityContext securityContext = ((SecurityContextHolder) parameter).getSecurityContext();
                    if (securityContext != null) {
                        return securityContext;
                    }
                }
            }
        }
        return null;
    }
}
