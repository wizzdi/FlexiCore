/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.init;

import com.flexicore.constants.Constants;
import com.flexicore.model.Category;
import com.flexicore.model.Clazz;
import com.flexicore.model.Tenant;
import com.flexicore.request.CategoryFilter;
import com.flexicore.request.ClazzFilter;
import com.flexicore.request.TenantFilter;
import com.flexicore.service.BaseclassService;
import com.flexicore.service.impl.ClassScannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * this is used to make sure everything has been initialized,
 *
 * @author Asaf
 */

@Configuration
public class Initializator  {

    private static final Logger logger = Logger.getLogger(Initializator.class.getCanonicalName());

    @Autowired
    private ClassScannerService classScannerService;

    @Autowired
    private Environment props;




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


    private static final boolean FLEXICORE_LICENSE = true;

    /**
     * this is called by the container upon init.
     */
    @Bean
    public StartingContext getStartingContext() throws Exception {

            initTests();


            logger.info("loading configuration");
            loadConstants();
            createFolderStructure();
            logger.info("registering classes");
            classScannerService.registerClasses();
            logger.info("Initializing classes");
           List<Clazz> clazzes= classScannerService.InitializeClazzes(); // must be done first!

            logger.info("Initializing operations");
            classScannerService.InitializeOperations();
            try {
                classScannerService.createDefaultObjects();
                if (FLEXICORE_LICENSE) {
                    classScannerService.registerFlexiCoreLicense();
                }
                classScannerService.createSwaggerTags();
                classScannerService.initializeInvokers();

                registerFilterClasses();


            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Error while initializing the system", ex);
            }
        return new StartingContext(clazzes);




    }


    private void registerFilterClasses() {
        BaseclassService.registerFilterClass(CategoryFilter.class, Category.class);
        BaseclassService.registerFilterClass(TenantFilter.class, Tenant.class);
        BaseclassService.registerFilterClass(ClazzFilter.class, Clazz.class);


    }

    private void initTests() {


    }


    private void createFolderStructure() {
        for (String path : Arrays.asList(Constants.ENTITIES_PATH, Constants.PLUGIN_PATH, Constants.UPLOAD_PATH, Constants.USERS_ROOT_DIRECTORY, Constants.PUBLIC_KEY)) {
            File file = new File(path);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    logger.log(Level.WARNING, "failed creating path: " + file);
                }
            }
        }


    }


    //TODO:save constant to config file so these can be changed.
    public void loadConstants() {

        Constants.SEPERATOR = "/";
        Constants.PLUGIN_PATH = props.getProperty("PluginPath", Constants.PLUGIN_PATH);
        Constants.ENTITIES_PATH = props.getProperty("EntitiesPath", Constants.ENTITIES_PATH);
        Constants.UPLOAD_PATH = props.getProperty("UploadPath", Constants.UPLOAD_PATH);
        Constants.UPLOAD_URL = props.getProperty("UploadURL", Constants.UPLOAD_URL);
        Constants.USERS_ROOT_DIRECTORY = props.getProperty("UsersRootDirectory", Constants.USERS_ROOT_DIRECTORY);
        Constants.FILE_CLEAN_INTERVAL = Long.parseLong(props.getProperty("FileCleanInterval", "" + Constants.FILE_CLEAN_INTERVAL));
        Constants.FIRST_RUN_FILE = props.getProperty("firstRunFile", Constants.FIRST_RUN_FILE);

        Constants.PUBLIC_KEY = props.getProperty("PublicKey", Constants.PUBLIC_KEY);
        Constants.jwtTokenSecretLocation = props.getProperty("jwtTokenSecretLocation", Constants.jwtTokenSecretLocation);

        Constants.enableHTTPErrorLog = Boolean.parseBoolean(props.getProperty("enableHTTPErrorLog", "" + Constants.enableHTTPErrorLog));
        Constants.maxHTTPLoggingBodyCharLength = Integer.parseInt(props.getProperty("maxHTTPLoggingBodyCharLength", "" + Constants.maxHTTPLoggingBodyCharLength));
        Constants.verificationLinkValidInMin = Integer.parseInt(props.getProperty("verificationLinkValidInMin", "" + Constants.verificationLinkValidInMin));
        Constants.userCacheMaxSize = Integer.parseInt(props.getProperty("userCacheMaxSize", "" + Constants.userCacheMaxSize));
        Constants.keySetFilePath = props.getProperty("keySetFilePath", Constants.keySetFilePath);
        Constants.timeShiftLocation = props.getProperty("timeShiftLocation", Constants.timeShiftLocation);
        Constants.JWTSecondsValid = Long.parseLong(props.getProperty("JWTSecondsValid", Constants.JWTSecondsValid + ""));
        Constants.showExceptionsInHttpResponse = Boolean.parseBoolean(props.getProperty("showExceptionsInHttpResponse", Constants.showExceptionsInHttpResponse + ""));
        Constants.hiddenHttpExceptionMessage = props.getProperty("hiddenHttpExceptionMessage", Constants.hiddenHttpExceptionMessage);
        String commaDelimitedWhiteListIpsForUnsecureApis = props.getProperty("commaDelimitedWhiteListIpsForUnsecureApis");
        Constants.commaDelimitedWhiteListIpsForUnsecureApis = commaDelimitedWhiteListIpsForUnsecureApis != null ? Stream.of(commaDelimitedWhiteListIpsForUnsecureApis.split(",")).collect(Collectors.toSet()) : Constants.commaDelimitedWhiteListIpsForUnsecureApis;


    }


}
