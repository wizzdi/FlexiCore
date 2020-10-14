/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.runningentities;

import com.flexicore.enums.ProcessPhase;
import com.flexicore.interfaces.ProccessPlugin;
import com.flexicore.model.Job;
import com.flexicore.model.Result;
import com.flexicore.service.impl.PluginService;
import org.pf4j.PluginManager;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class JobProcessor implements ItemProcessor<Job, Job> {

    @Autowired
    @Lazy
    private PluginManager pluginManager;

    private Logger logger = Logger.getLogger(getClass().getCanonicalName());


    @Override
    public Job process(Job job) throws Exception {
        job.setCurrentPhase(ProcessPhase.Processing.getName());
        if (!processJob(job)) {
            job.setBatchStatus(BatchStatus.FAILED);
            job.setCurrentPhase(ProcessPhase.Error.getName());
            throw new Exception("job was not analyzed successfully by any available plugin" +
                    "Job :" + job);
        }
        job.setCurrentPhase(ProcessPhase.Processed.getName());


        return job;
    }


    /**
     * find the correct FC PI to process the job.
     *
     * @param job
     * @return true if job was found
     */
    private boolean processJob(com.flexicore.model.Job job) {
        Collection<?> analyzers = pluginManager.getExtensions(job.getJobInformation().getHandler());
        int size = analyzers.size();
        int i = 1;
        Result res = null;
        boolean found = false;
        ProccessPlugin plugin;
        Iterator<?> iter = analyzers.iterator();
        while (iter.hasNext() && !found) { //by definition, all PI run unless otherwise set by one of them.
            Object p = iter.next();
            if (p instanceof ProccessPlugin) {
                plugin = (ProccessPlugin) p;
                try {
                    plugin.process(job); //call the Process method of the PI
                    res = job.getJobInformation().getCurrrentPhaseResult();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "exception while analyzing", e);
                }

                job.setCurrentPhasePrecentage((float) i / size);
                if (res != null && res.isSucceeded() ) {
                    found = true; //PI has succeeded and it wants to be the only one who process the job
                    job.setCurrentPhasePrecentage(1);
                }
                i++;
            }


        }
        if (res == null || !res.isSucceeded()) {
            job.setCurrentPhase(ProcessPhase.Error.getName());
            job.setCurrentPhasePrecentage(1);
            return false; //no PI has managed to process successfully the Job.
        } else {
            return true;
        }


    }


}
