/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.service.impl;

import com.flexicore.annotations.plugins.PluginInfo;
import com.flexicore.data.PluginRepository;
import com.flexicore.data.jsoncontainers.PluginInformationHolder;
import com.flexicore.data.jsoncontainers.PluginType;

import com.flexicore.interfaces.Plugin;
import com.flexicore.model.Baseclass;
import com.flexicore.model.Job;
import com.flexicore.model.ModuleManifest;
import com.flexicore.model.PluginRequirement;
import com.flexicore.utils.FlexiCore;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.pf4j.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.xml.sax.SAXException;

import javax.enterprise.context.ApplicationScoped;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import javax.inject.Named;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Named

@Primary
@Component
public class PluginService implements com.flexicore.service.PluginService {
    @Autowired
    private PluginRepository pluginRepository;

   private Logger logger = Logger.getLogger(getClass().getCanonicalName());

    @Autowired
    private FileResourceService fileResourceService;

    @Autowired
    private GenericWebApplicationContext context;

    @Autowired
    private PluginManager pluginManager;


    /**
     * @param pluginType
     * @param requirements
     * @return a list of instances which implement pluginType
     */
    @Override
    public Collection<?> getPlugins(Class<? extends Plugin> pluginType,
                                    HashMap<String, PluginRequirement> requirements, Job orderObject) {
        return pluginManager.getExtensions(pluginType);
    }


    public <T> T instantiateNonPlugin(Class<T> c) {
        return instansiate(c,new HashMap<>());
    }

    @Override
    public List<ModuleManifest> getModelListing() {
        return pluginRepository.getModelListing();
    }


    /**
     * @param plugin  - class implementing Plugin interface
     * @param type    - Type of Plugin aka interface that extends Plugin
     * @param moduleManifest
     */
    public <T extends Plugin> void addPlugin(Class<T> plugin, Class<T> type, ModuleManifest moduleManifest, PluginInfo info) {
        addPlugin(plugin,type,moduleManifest,info,false);
    }

    public <T extends Plugin> void addPlugin(Class<T> plugin, Class<T> type, ModuleManifest moduleManifest, PluginInfo info,boolean internal) {
        context.registerBean(plugin);

        // pluginRepository.addPlugin(plugin, type, moduleManifest, info);

    }










    public File addJarToPersistenceContext(String jarPath) {
        try {
            return pluginRepository.addJarToPersistenceContext(jarPath);
        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            logger.log(Level.WARNING, "unable to move jar to Persistence Folder", e);
        }
        return null;
    }


    @Override
    public <T> T instansiate(Class<T> type, Map<String, PluginRequirement> reqs) {
        List<T> extensions = pluginManager.getExtensions(type);
        return extensions.isEmpty()?null:extensions.get(0);

    }






    public <T> T instansiateREST(Class<T> type, Map<String, PluginRequirement> reqs) {
        String[] name = type.getCanonicalName().split("\\$");
        //Class<T> clazz = (Class<T>) pluginRepository.getPluginClassByRequirement(name[0], reqs);
        return instansiate(type, reqs);
    }

    public <T> void registerBean(Class<T> type) {
        context.registerBean(type);
    }

    @Override
    public Set<ModuleManifest> getPluginModuleListing(){
        return Collections.unmodifiableSet(pluginRepository.getPluginListing().keySet());
    }

    @Override
    public List<PluginInformationHolder> getAll() {
        Map<ModuleManifest, Map<Class<?>, List<Class<?>>>> byType = pluginRepository.getPluginListing();
        Map<Class<?>, List<Class<?>>> plugins;
        List<PluginInformationHolder> info = pluginRepository.getModelListing().parallelStream().map(f -> new PluginInformationHolder("", "", -1, f)).collect(Collectors.toList());
        info.addAll(PluginService.externalModules.parallelStream().map(f->new PluginInformationHolder("","",-1,f)).collect(Collectors.toList()));
        for (Entry<ModuleManifest, Map<Class<?>, List<Class<?>>>> entry : byType.entrySet()) {
            plugins = entry.getValue();
            if (plugins != null) {
                for (Entry<Class<?>, List<Class<?>>> ofType : plugins.entrySet()) {
                    for (Class<?> class1 : ofType.getValue()) {
                        PluginInfo versionInfo = class1.getDeclaredAnnotation(PluginInfo.class);
                        if (versionInfo != null) {
                            info.add(new PluginInformationHolder(class1.getCanonicalName(), ofType.getKey().getCanonicalName(), versionInfo.version(), entry.getKey()));

                        }
                    }


                }
            }

        }

        return info;

    }


    @Override
    public ModuleManifest readModule(File jar, Logger logger) {

        PluginType pluginType = PluginType.Service;
        String originalPath = jar.getAbsolutePath();
        ModuleManifest moduleManifest = new ModuleManifest(jar.getAbsolutePath(), originalPath, pluginType);
        try {
            JarFile jarFile = new JarFile(jar);
            JarEntry jarEntry = jarFile.getJarEntry("flexicore-manifest.mf");
            if(jarEntry==null){
                logger.log(Level.SEVERE,"missing manifest File For: "+jar.getAbsolutePath());
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


    public void loadModelListing() {
        pluginRepository.loadModelListing();
    }

    @Override
    public boolean cleanUpInstance(Object o) {
        return true;
    }




    public <T extends Plugin> Class<T> isPlugin(Class<?> c) {
        Class<?>[] directInterfaces = c.getInterfaces();
        Class<Plugin> pluginClass = Plugin.class;
        Class<T> type = null;
        if (!c.isInterface()) {
            for (Class<?> direct : directInterfaces) {
                List<Class<?>> interfaces = ClassUtils.getAllInterfaces(direct);
                if (interfaces.contains(pluginClass)) {
                    type = (Class<T>) direct;

                }

            }
        }
        if (type == null) {
            c = c.getSuperclass();
            if (c != null) {
                type = isPlugin(c);
            }

        }

        return type;
    }



    public static File getTemporaryFile(String prefix, String suffix) {

        return new File(System.getProperty("java.io.tmpdir"), prefix + UUID.randomUUID().toString() + System.currentTimeMillis() + suffix);


    }


    public boolean isUpdateForced() {
        File classPath = new File(FlexiCore.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        File forceFile = new File(classPath.getAbsoluteFile() + File.separator + "plugin.force");
        return forceFile.exists();
    }

    public void createForceFile() throws IOException {
        File classPath = new File(FlexiCore.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        File forceFile = new File(classPath.getAbsoluteFile() + File.separator + "plugin.force");
        if (!forceFile.exists()) {
            forceFile.createNewFile();

        }
    }

    public void deleteForceFile() {
        File classPath = new File(FlexiCore.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        File forceFile = new File(classPath.getAbsoluteFile() + File.separator + "plugin.force");
        if (forceFile.exists()) {
            forceFile.delete();
        }
    }
}


