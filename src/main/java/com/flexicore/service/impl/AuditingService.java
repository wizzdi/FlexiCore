/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexicore.data.AuditingRepository;
import com.flexicore.data.jsoncontainers.BasicContainer;
import com.flexicore.data.jsoncontainers.ListHolder;
import com.flexicore.data.jsoncontainers.ObjectMapperContextResolver;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.events.PluginsLoadedEvent;
import com.flexicore.model.Baseclass;
import com.flexicore.model.FilteringInformationHolder;
import com.flexicore.model.Operation;
import com.flexicore.model.User;
import com.flexicore.model.auditing.*;
import com.flexicore.request.AuditingFilter;
import com.flexicore.security.AuthenticationRequestHolder;
import com.flexicore.security.SecurityContext;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.enterprise.event.ObservesAsync;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Primary
@Component
public class AuditingService implements com.flexicore.service.AuditingService {

    @Autowired
    private AuditingRepository auditingRepository;

    @Autowired
    private BaseclassNewService baseclassNewService;


    private Logger logger = Logger.getLogger(getClass().getCanonicalName());
    private static final AtomicBoolean init = new AtomicBoolean(false);
    private static AuditingWriter auditingWriter;

    static {
        containerMap.put(FilteringInformationHolder.class.getCanonicalName(), i -> i);
        containerMap.put(PaginationResponse.class.getCanonicalName(), o -> new PaginationAuditingContainer((PaginationResponse) o));
        containerMap.put(Baseclass.class.getCanonicalName(), f -> new BasicContainer((Baseclass) f));
        containerMap.put(String.class.getCanonicalName(), i -> i);
        containerMap.put(Integer.class.getCanonicalName(), i -> i);
        containerMap.put(int.class.getCanonicalName(), i -> i);
        containerMap.put(Double.class.getCanonicalName(), i -> i);
        containerMap.put(double.class.getCanonicalName(), i -> i);
        containerMap.put(Long.class.getCanonicalName(), i -> i);
        containerMap.put(long.class.getCanonicalName(), i -> i);
        containerMap.put(List.class.getCanonicalName(),f->new ListHolder((List) f));
        containerMap.put(AuthenticationRequestHolder.class.getCanonicalName(),f->new AuthenticationRequestHolder((AuthenticationRequestHolder) f));


    }

    @EventListener
    public void init(PluginsLoadedEvent e) {
        if (init.compareAndSet(false, true)) {
            auditingWriter = new AuditingWriter();
            new Thread(auditingWriter).start();
        }
    }


    @Override
    public void merge(AuditingEvent o) {
        auditingRepository.merge(o);
    }

    @Override
    public void addAuditingJob(AuditingJob auditingJob) {
        auditingWriter.addEvent(auditingJob);
    }

    @Async
    @EventListener
    public void handleAuditingEvent( AuditingEvent auditingEvent){
        auditingRepository.merge(auditingEvent);
        logger.info("Call to "+auditingEvent.getOperationHolder() +" was Audited");
    }

    @Async
    @EventListener
    public void createAuditingEvent(AuditingJob auditingJob) throws JsonProcessingException {
        ObjectMapper objectMapper=ObjectMapperContextResolver.getDefaultMapper();
        boolean skipFirst= auditingJob.getInvocationContext().getMethod()!=null&&isFirstAuthToken(auditingJob.getInvocationContext().getMethod().getParameters());
        Stream<Object> parameters = Stream.of(auditingJob.getInvocationContext().getParameters());
        if(skipFirst){
            parameters=parameters. skip(1);
        }
        List<Object> list = parameters.map(com.flexicore.service.AuditingService::contain).filter(Objects::nonNull).collect(Collectors.toList());

        Document requestDoc = Document.parse(objectMapper.writeValueAsString(new RequestHolder(list)));
        Document responseDoc = Document.parse(objectMapper.writeValueAsString(new ResponseHolder(com.flexicore.service.AuditingService.contain(auditingJob.getResponse()))));

        SecurityContext securityContext = auditingJob.getSecurityContext();
        AuditingEvent auditingEvent = new AuditingEvent()
                .setDateOccurred(auditingJob.getDateOccured())
                .setOperationHolder(securityContext !=null&&securityContext.getOperation()!=null?new OperationHolder(securityContext.getOperation()):null)
                .setUserHolder(securityContext !=null&&securityContext.getUser()!=null?new UserHolder(securityContext.getUser()):null)
                .setRequest(requestDoc)
                .setResponse(responseDoc)
                .setTimeTaken(auditingJob.getTimeTaken())
                .setAuditingType(auditingJob.getAuditingType())
                .setFailed(auditingJob.isFailed());
        auditingRepository.merge(auditingEvent);
        logger.info("Call to "+auditingEvent.getOperationHolder() +" was Audited");
    }

    private boolean isFirstAuthToken( Parameter[] parameters) {
        if(parameters.length > 0 ){
            Parameter parameter = parameters[0];
            HeaderParam headerParam= parameter.getAnnotation(HeaderParam.class);
            if(headerParam!=null){
                return "authenticationkey".toLowerCase().equals(headerParam.value().toLowerCase());
            }
            PathParam pathParam= parameter.getAnnotation(PathParam.class);

            if(pathParam!=null){
                return "authenticationkey".toLowerCase().equals(pathParam.value().toLowerCase());
            }

            QueryParam queryParam= parameter.getAnnotation(QueryParam.class);

            if(queryParam!=null){
                return "authenticationkey".toLowerCase().equals(queryParam.value().toLowerCase());
            }
        }
        return false;
    }

    @Override
    public PaginationResponse<AuditingEvent> getAllAuditingEvents(AuditingFilter auditingFilter, SecurityContext securityContext) {
        List<AuditingEvent> auditingEvents=auditingRepository.listAllAuditingEvents(auditingFilter);
        long count =auditingRepository.countAllAuditingEvents(auditingFilter);
        return new PaginationResponse<>(auditingEvents,auditingFilter,count);
    }
    public void validate(AuditingFilter auditingFilter, SecurityContext securityContext) {
        Set<String> operationIds=auditingFilter.getOperationIds().stream().map(f->f.getId()).collect(Collectors.toSet());
        Map<String, Operation> operationMap=operationIds.isEmpty()?new HashMap<>():baseclassNewService.listByIds(Operation.class,operationIds,securityContext).stream().collect(Collectors.toMap(f->f.getId(), f->f));
        operationIds.removeAll(operationMap.keySet());
        if(!operationIds.isEmpty()){
            throw new BadRequestException("No Operations with ids "+operationIds);
        }
        auditingFilter.setOperations(new ArrayList<>(operationMap.values()));

        Set<String> userIds=auditingFilter.getUserIds().stream().map(f->f.getId()).collect(Collectors.toSet());
        Map<String, User> userMap=userIds.isEmpty()?new HashMap<>():baseclassNewService.listByIds(User.class,userIds,securityContext).stream().collect(Collectors.toMap(f->f.getId(), f->f));
        userIds.removeAll(userMap.keySet());
        if(!userIds.isEmpty()){
            throw new BadRequestException("No User with ids "+userIds);
        }
        auditingFilter.setUsers(new ArrayList<>(userMap.values()));
    }

    class AuditingWriter implements Runnable {
        private LinkedBlockingQueue<AuditingJob> linkedBlockingQueue = new LinkedBlockingQueue<>();
        private boolean stop;

        @Override
        public void run() {
            logger.info("Auditing Writer Started");
            while (!stop) {
                try {
                    AuditingJob auditingJob = linkedBlockingQueue.poll(5000, TimeUnit.MILLISECONDS);
                    if (auditingJob != null) {
                        try {
                            createAuditingEvent(auditingJob);
                        } catch (Exception e) {
                            logger.log(Level.SEVERE, "Exception while creating auditing", e);
                        }

                    }
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "interrupted while waiting for auditing jobs", e);
                }
            }
            logger.info("Auditing Writer Stopped");

        }

        public void addEvent(AuditingJob auditingJob) {
            linkedBlockingQueue.add(auditingJob);
        }
    }
}
