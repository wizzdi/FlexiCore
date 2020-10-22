/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.init;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * this is used to make sure everything has been initialized,
 *
 * @author Asaf
 */

@Configuration
public class Initializator {

    private static final Logger logger = Logger.getLogger(Initializator.class.getCanonicalName());

    @Value("${flexicore.entities:/home/flexicore/entities}")
    private String entitiesPath;
    @Value("${flexicore.plugins:/home/flexicore/plugins}")
    private String pluginsPath;
    @Value("${flexicore.upload:/home/flexicore/upload}")
    private String uploadPath;
    @Value("${flexicore.users.rootDirPath:/home/flexicore/users/}")
    private String usersPath;


    private static AtomicBoolean initFully = new AtomicBoolean(false);

    public static boolean getInit() {
        return initFully.get();
    }

    public static void setInit() {
        initFully.compareAndSet(false, true);
    }

    public static void setRestarting() {
        initFully.compareAndSet(true, false);
    }


    /**
     * this is called by the container upon init.
     *
     * @return the starting context
     * @throws Exception if there is any issue starting the context
     */
    @Bean
    public StartingContext getStartingContext() throws Exception {

        createFolderStructure();

        return new StartingContext(System.currentTimeMillis());


    }



    private void createFolderStructure() {
        for (String path : Arrays.asList(entitiesPath, pluginsPath, uploadPath, usersPath)) {
            File file = new File(path);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    logger.log(Level.WARNING, "failed creating path: " + file);
                }
            }
        }


    }


}
