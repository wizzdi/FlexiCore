/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.service.impl;

import com.flexicore.enums.ProcessPhase;
import com.flexicore.init.FlexiCoreExtensionFactory;
import com.flexicore.interfaces.AnalyzerPlugin;
import com.flexicore.interfaces.ProccessPlugin;
import com.flexicore.model.Job;
import com.flexicore.model.JobInformation;
import com.flexicore.model.PluginRequirement;
import com.flexicore.model.User;
import com.flexicore.request.RegisterForJobUpdates;
import com.flexicore.security.SecurityContext;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.websocket.Session;
import javax.ws.rs.BadRequestException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


@Component
@Primary
public class JobService implements com.flexicore.service.JobService {
    private static Map<String, Map<String, Session>> jobListeners = new ConcurrentHashMap<>();
    private static final Cache<String, Job> jobs = CacheBuilder.newBuilder().maximumSize(1000).expireAfterAccess(2, TimeUnit.HOURS).build();
    private static final Cache<String, Job> startedJobs = CacheBuilder.newBuilder().maximumSize(1000).expireAfterAccess(2, TimeUnit.HOURS).build();


    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private org.springframework.batch.core.Job job;
    private Logger logger = LoggerFactory.getLogger(FlexiCoreExtensionFactory.class);


    @Override
    public Job checkJobStatus(String id) {
        Job processJob = get(id);

        long jid = processJob.getBatchJobId();
        JobExecution jobExecution=jobExplorer.getJobExecution(jid);
        if(jobExecution!=null){
            processJob.setBatchStatus(jobExecution.getStatus());
        }
        return processJob;
    }

    @Override
    public boolean checkJobValidity(Job job) {
        return (job != null && job.getJobInformation() != null && job.getJobInformation().getHandler() != null);
    }

    @Override
    public void setJobDefualts(Job job) {
        job.setCurrentPhase(ProcessPhase.Waiting.getName());
        job.setCurrentPhasePrecentage(0);

    }

    @Override
    public void startJob(Job job, SecurityContext securityContext) {
        if (checkJobValidity(job)) {
            setJobDefualts(job);
            putFileProcessJob(job);

            job.setSecurityContext(securityContext);
            Properties prop = new Properties();
            prop.setProperty("fileProcessJobId", job.getId());
            try {
                JobParameters jobParameters = new JobParametersBuilder()
                        .addString("jobId", job.getId(), true).toJobParameters();
                org.springframework.batch.core.JobExecution jobExecution=jobLauncher.run(this.job, jobParameters);
                job.setBatchJobId(jobExecution.getJobId());
            } catch (Exception e) {
                logger.error("failed starting job", e);
            }

        }

    }

    @Override
    public Job startJob(Serializable content, Class<? extends ProccessPlugin> type,
                        Properties jobprops, HashMap<String, PluginRequirement> requriments, SecurityContext securityContext) {
        Job job;
        User user = null;
        if (securityContext != null) {
            user = securityContext.getUser();
        }
        if (requriments == null) {
            requriments = new HashMap<>();
        }
        if (jobprops == null) {
            jobprops = new Properties();
        }
        job = new Job();

        JobInformation info = new JobInformation();
        info.setJobInfo(content);
        info.setHandle(true); // tells the PI system to read the next Cycle.
        info.setHandler(type); // the first PI to run (or
        // multiple of) will be an
        // Analyzer PI
        info.setJobProperties(jobprops);
        info.setRequirments(requriments);
        job.setCurrentPhase(ProcessPhase.Waiting.getName());
        job.setCurrentPhasePrecentage(0);
        job.setJobInformation(info);
        job.setSecurityContext(securityContext); // We need to know who has
        startJob(job, securityContext);
        return job;
    }

    @Override
    public void putFileProcessJob(Job job) {
        put(job);
    }

    @Override
    public void put(Job job) {
        jobs.put(job.getId(), job);
    }

    @Override
    public Job get(String id) {
        return jobs.getIfPresent(id);
    }

    public synchronized static Job readJob(String id) {
        Job job= jobs.getIfPresent(id);
        if(job!=null&&startedJobs.getIfPresent(id)==null){
            startedJobs.put(id,job);
            return job;
        }
        return null;

    }



    @Override
    public Job getProcess(String id) {
        return get(id);
    }


    @Override
    public void updateJobProperty(String id, String key, String value) {
        Job processJob = get(id);
        Properties props = processJob.getJobInformation().getJobProperties();
        if (props == null) {
            props = new Properties();
            processJob.getJobInformation().setJobProperties(props);
        }
        props.setProperty(key, value);

    }

    @Override
    public void cancel(String id) {
        Job processJob = get(id);
        long jid = processJob.getBatchJobId();
        JobOperator jo = BatchRuntime.getJobOperator();
        jo.abandon(jid);
    }

    @Override
    public void updateJobPhase(String jobID, String phaseName) {
        Job job = get(jobID);
        job.setCurrentPhase(phaseName);
    }

    @Override
    public void registerForJobUpdates(RegisterForJobUpdates registerForJobUpdates, SecurityContext securityContext) {
        Job job = get(registerForJobUpdates.getJobId());
        if (job == null) {
            throw new BadRequestException("No Job with id " + registerForJobUpdates.getJobId());
        }
        Session session = sessionMap.get(registerForJobUpdates.getWebSocketSessionId());
        if (session == null) {
            throw new BadRequestException("No Session with id " + registerForJobUpdates.getWebSocketSessionId());
        }
        jobListeners.computeIfAbsent(job.getId(), f -> new ConcurrentHashMap<>()).put(session.getId(), session);

    }

    public Job test(SecurityContext securityContext) {


        return startJob(null, AnalyzerPlugin.class, null, null, securityContext);
    }
}
