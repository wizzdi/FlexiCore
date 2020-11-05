/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.init;

import com.flexicore.model.Clazz;
import com.flexicore.model.Tenant;
import com.flexicore.request.ClazzFilter;
import com.flexicore.request.TenantFilter;
import com.flexicore.service.BaseclassService;
import com.flexicore.service.impl.ClassScannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this is used to make sure everything has been initialized,
 *
 * @author Asaf
 */

@Configuration
public class Initializator {

    private static final Logger logger = LoggerFactory.getLogger(Initializator.class.getCanonicalName());

    @Autowired
    private ClassScannerService classScannerService;

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
        logger.info("registering classes");
        classScannerService.registerClasses();
        logger.info("Initializing classes");
        List<Clazz> clazzes = classScannerService.InitializeClazzes(); // must be done first!

        logger.info("Initializing operations");
        classScannerService.InitializeOperations();
        try {
            classScannerService.createDefaultObjects();
            classScannerService.createSwaggerTags();
            classScannerService.initializeInvokers();

            registerFilterClasses();


        } catch (Exception ex) {
            logger.error( "Error while initializing the system", ex);
        }
        return new StartingContext(clazzes);


    }


    private void registerFilterClasses() {
        BaseclassService.registerFilterClass(TenantFilter.class, Tenant.class);
        BaseclassService.registerFilterClass(ClazzFilter.class, Clazz.class);


    }


    private void createFolderStructure() {
        for (String path : Arrays.asList(entitiesPath, pluginsPath, uploadPath, usersPath)) {
            File file = new File(path);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    logger.warn( "failed creating path: " + file);
                }
            }
        }


    }


}
