/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexicore.annotations.InheritedComponent;
import com.flexicore.data.jsoncontainers.PluginType;
import com.flexicore.interfaces.Plugin;
import com.flexicore.model.ModuleManifest;
import com.flexicore.utils.FlexiCore;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.enterprise.context.spi.CreationalContext;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;


@Singleton
@Named
@InheritedComponent
public class PluginRepository {


    // TODO: register a plugin upon request and not upon discovery set TTL by
    // parameters in plugin interface

    // stores a list of PI by their type (the interface they implement)
    private Map<Class<?>, List<Class<?>>> byType = new HashMap<>();

    // stores a PI by its class name, keep the same class in multiple versions.
    private Map<String, ConcurrentSkipListMap<Integer, Class<?>>> byClassName = new HashMap<>();
    // stores plug-in by Jar, so when the Jar is deleted, the lists can be
    // updated.
    private Map<String, List<Class<?>>> byJar = new HashMap<>();
    // stores instantiated plug-ins
    private Map<Class<?>, Object> loadedPlugins = new HashMap<>();
    // stores interfaces (which are plug-ins) accessible by class
    private Map<Class<?>, Class<?>> pluginClassToPluginType = new HashMap<>();
    // stores jars accessible by Plug-in class and version
    private Map<Class<?>, Map<String, Integer>> jarPathToVersion = new HashMap<>();
    private Map<ModuleManifest, Map<Class<?>, List<Class<?>>>> pluginListing = new HashMap<>();
    private List<ModuleManifest> modelListing = new ArrayList<>();

    // Stores instantiated plug-ins by time to live (in ms)
    private ConcurrentSkipListMap<Long, Plugin> TTLmap = new ConcurrentSkipListMap<>();
   private Logger logger = Logger.getLogger(getClass().getCanonicalName());
    private AtomicBoolean beanManagerReady = new AtomicBoolean(false);
    // private List<Bean<?>> beanList=null;
    // private ArrayList<Object> newBeans=null;
    // private HashMap<Type, ArrayList<Bean<?>>> typeMap=null;
    private static Map<String, Boolean> checkedAnnotaions = new ConcurrentHashMap<>();
    private static Map<String, CreationalContext<?>> hashcodeToCreationalContext = new ConcurrentHashMap<>();


    private ObjectMapper objectMapper = new ObjectMapper();
    private static Set<String> initCalled = new ConcurrentSkipListSet<>();


    public PluginRepository() {


    }


/*
    public void refreshEntityManager() {
		if (factory instanceof JpaEntityManagerFactory) {
			((JpaEntityManagerFactory) factory).refreshMetadata(null);
		}


	}*/


    public List<ModuleManifest> getModelListing() {
        return modelListing;
    }


    public File getJarByPluginUUID(String uuid) {
        for (ModuleManifest moduleManifest : pluginListing.keySet()) {
            if (moduleManifest.getUuid().equals(uuid)) {
                return new File(moduleManifest.getOriginalModuleLocation());
            }
        }
        return null;

    }

    public File getJarByModelUUID(String uuid) {
        for (ModuleManifest moduleManifest : modelListing) {
            if (moduleManifest.getUuid().equals(uuid)) {
                return new File(moduleManifest.getOriginalModuleLocation());
            }
        }
        return null;

    }


    public Map<ModuleManifest, Map<Class<?>, List<Class<?>>>> getPluginListing() {
        return Collections.unmodifiableMap(pluginListing);
    }


    private void addClassToPersistenceXml(Set<String> toAdd, String persistenceUnitName) throws ParserConfigurationException, IOException, SAXException, TransformerException {


        File f = new File(FlexiCore.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        String persistenceLocation = f.getAbsolutePath() + "/META-INF/persistence.xml";
        File fXmlFile = new File(persistenceLocation);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile);
        Element d = doc.getDocumentElement();
        NodeList l = d.getElementsByTagName("persistence-unit");
        int selected = -1;

        for (int i = 0; i < l.getLength(); i++) {
            if (l.item(i).getAttributes().getNamedItem("name").getNodeValue().equals(persistenceUnitName)) {
                selected = i;
                break;
            }
        }
        if (selected == -1) {
            logger.warning("No Persistence Unit named " + persistenceUnitName + " at " + persistenceLocation);
            return;
        }
        Node n = l.item(selected);
        l = ((Element) n).getElementsByTagName("class");
        Node exlcudeTag = ((Element) n).getElementsByTagName("exclude-unlisted-classes").item(0);

        for (int j = 0; j < l.getLength(); j++) {
            toAdd.remove(l.item(j).getFirstChild().getNodeValue());
        }
        for (String s : toAdd) {
            Element el = doc.createElement("class");
            el.appendChild(doc.createTextNode(s));
            n.insertBefore(el, exlcudeTag);
            //n.appendChild(el);
        }
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(fXmlFile);
        // creating output stream
        transformer.transform(source, result);

    }

    /**
     * copies jar from jarPath to project/lib folder ,changes persistence.xml
     * accordingly and refreshes entity manager
     *
     * @param jarPath
     * @return file representing the copied jar
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws TransformerException
     */
    public File addJarToPersistenceContext(String jarPath)
            throws ParserConfigurationException, SAXException, IOException, TransformerException {

        File f = new File(FlexiCore.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile()
                .getParentFile().getParentFile();
        String persistenceLocation = f.getAbsolutePath() + "/META-INF/persistence.xml";
        String path = f.getPath() + "/lib";
        File existing = new File(jarPath);
        File jarFileAtLib = new File(path + "/" + existing.getName());
        Files.copy(existing.toPath(), jarFileAtLib.toPath(), StandardCopyOption.REPLACE_EXISTING);
        File fXmlFile = new File(persistenceLocation);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile);
        Element d = doc.getDocumentElement();
        NodeList l = d.getChildNodes();
        int i;
        for (i = 0; i < l.getLength(); i++) {
            if (l.item(i).getNodeName().equalsIgnoreCase("persistence-unit")) {
                break;
            }
        }
        Node n = l.item(i);

        Element e = doc.createElement("jar-file");
        e.appendChild(doc.createTextNode("file:" + jarFileAtLib.getAbsolutePath()));

        n.appendChild(e);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute("{http://xml.apache.org/xslt}indent-amount", 2);

        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(fXmlFile);
        // creating output stream
        transformer.transform(source, result);
        //refreshEntityManager();
        return jarFileAtLib;

    }

    public static boolean isQualifier(Class<? extends Annotation> annotation) {
        Boolean isQualifier = checkedAnnotaions.get(annotation.getCanonicalName());
        if (isQualifier != null) {
            return isQualifier;
        }
        checkedAnnotaions.put(annotation.getCanonicalName(), false);
        if (annotation.equals(Qualifier.class)) {
            checkedAnnotaions.put(annotation.getCanonicalName(), true);
            return true;
        } else {
            Annotation[] annotations = annotation.getDeclaredAnnotations();
            for (Annotation annotation2 : annotations) {
                if (isQualifier(annotation2.annotationType())) {
                    checkedAnnotaions.put(annotation.getCanonicalName(), true);
                    return true;
                }

            }
            return false;
        }
    }


    public void loadModelListing() {
        File classPath = new File(FlexiCore.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        File modelManifestsFolder = new File(classPath, "modelManifests");
        File[] files = modelManifestsFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    ModuleManifest moduleManifest = objectMapper.readValue(file, ModuleManifest.class);
                    modelListing.add(moduleManifest);

                } catch (IOException e) {
                    logger.log(Level.SEVERE, "unable to parse manifest", e);
                }
            }
        }

    }

    private void updateLoadedModelsFolder(ModuleManifest moduleManifest) throws IOException {
        File classPath = new File(FlexiCore.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        File modelManifestsFolder = new File(classPath, "modelManifests");
        modelManifestsFolder.mkdirs();
        File manifest = new File(modelManifestsFolder, moduleManifest.getUuid() + ".mf");
        objectMapper.writeValue(manifest, moduleManifest);
    }

    private void copyJarEntry(JarEntry entry, JarFile jar, File classAtClassPath) throws IOException {

        classAtClassPath.getParentFile().mkdirs();
        if (classAtClassPath.exists()) {
            classAtClassPath.delete();
        }
        InputStream is = jar.getInputStream(entry);
        Files.copy(is, classAtClassPath.toPath());
        is.close();

    }





}