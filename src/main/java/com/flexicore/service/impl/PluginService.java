/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.service.impl;

import com.flexicore.data.jsoncontainers.PluginType;
import com.flexicore.interfaces.Plugin;
import com.flexicore.model.Job;
import com.flexicore.model.ModuleManifest;
import org.pf4j.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Primary
@Component
public class PluginService implements com.flexicore.service.PluginService {


    private Map<ModuleManifest, Map<Class<?>, List<Class<?>>>> pluginListing = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(PluginService.class);

    @Lazy
    @Autowired
    private PluginManager pluginManager;



    @Override
    public List<ModuleManifest> getModelListing() {
        return new ArrayList<>();
    }



    @Override
    public Set<ModuleManifest> getPluginModuleListing(){
        return Collections.unmodifiableSet(Collections.unmodifiableMap(pluginListing).keySet());
    }

    @Override
    public ModuleManifest readModule(File jar, java.util.logging.Logger logger) {

        PluginType pluginType = PluginType.Service;
        String originalPath = jar.getAbsolutePath();
        ModuleManifest moduleManifest = new ModuleManifest(jar.getAbsolutePath(), originalPath, pluginType);
        try {
            JarFile jarFile = new JarFile(jar);
            JarEntry jarEntry = jarFile.getJarEntry("flexicore-manifest.mf");
            if(jarEntry==null){
                logger.severe("missing manifest File For: "+jar.getAbsolutePath());
                return moduleManifest;
            }
            InputStream inputStream = jarFile.getInputStream(jarEntry);
            Properties properties = new Properties();
            properties.load(inputStream);
            HashSet<String> requires = Stream.of(properties.getProperty("requires", "").split(",")).filter(s->s!=null&&!s.isEmpty()).map(f->f.trim()).collect(Collectors.toCollection(HashSet::new));
            HashSet<String> basicProvides = Stream.of(properties.getProperty("provides", "").split(",")).filter(s->s!=null&&!s.isEmpty()).map(f->f.trim()).collect(Collectors.toCollection(HashSet::new));
            String uuid=properties.getProperty("uuid","unknown"+UUID.randomUUID());
            String version=properties.getProperty("version","unknown"+UUID.randomUUID());

         /*   HashSet<String> provides= jarFile.stream().filter(f -> PluginLoader.isClassProvided(f.getName().replaceAll("/", "."), basicProvides)).map(f -> StringUtils.removeEnd(f.getName().replaceAll("/", "."), ".class")).collect(Collectors.toCollection(HashSet::new));
            moduleManifest.set(uuid,version,jar.getAbsolutePath(),originalPath,requires, provides,pluginType);
*/

        } catch (IOException e) {
            logger.log(Level.SEVERE,"unable to get dependencies",e);
        }
        return moduleManifest;
    }



}


