/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.runningentities;

import com.flexicore.annotations.OperationsInside;
import com.flexicore.annotations.plugins.PluginInfo;
import com.flexicore.annotations.plugins.TestsInside;
import com.flexicore.constants.Constants;
import com.flexicore.data.PluginRepository;
import com.flexicore.data.TestsRepository;
import com.flexicore.data.jsoncontainers.PluginType;
import com.flexicore.exceptions.MissingDependencies;
import com.flexicore.init.Initializator;
import com.flexicore.interfaces.Plugin;
import com.flexicore.interfaces.dynamic.InvokerInfo;
import com.flexicore.model.JarPlugin;
import com.flexicore.model.ModuleManifest;
import com.flexicore.rest.JaxRsActivator;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.ClassScannerService;
import com.flexicore.service.impl.PluginService;
import com.flexicore.service.impl.SecurityService;
import com.flexicore.utils.FlexiCore;
import com.flexicore.utils.InheritanceUtils;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import javax.enterprise.context.control.RequestContextController;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicatorRegistry;

import javax.inject.Named;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.ws.rs.ext.Provider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * loads plugins on start and when a new one is added to spesified folder
 *
 * @author Asaf
 */

@Named

public class PluginLoader implements Runnable {

    private Path pluginDir;
    private Path tempPluginDir;
    private static GenericJarLoader jarLoader;
    private GenericFilter filter;
   private Logger logger = Logger.getLogger(getClass().getCanonicalName());

    @Autowired
    private PluginService pluginService;
    private RemovableURLClassLoader parent;

    @Autowired
    private ClassScannerService classScannerService;


    @Autowired
    private SecurityService securityService;
    @Autowired
    private HealthIndicatorRegistry healthContributorRegistry;


    /*@Autowired
    @Named(value = "LocalHealth")
    private SmallRyeHealthReporter smallRyeHealthReporter;*/


    private ServerContainer serverContainer;

    private Queue<URLClassLoader> classLoaders = new ConcurrentLinkedQueue<>();
    private boolean stop;


    public PluginLoader() {
        pluginDir = null;
        String[] strings = {".jar"};
        filter = new GenericFilter(strings);
        URL[] urls = {};
        // in order to free resources loaded by this Jar,
        // we need to keep it on a list and release all of the dependencies and
        // the list of classes included in the Jar
        if (logger == null) {
            logger = Logger.getLogger(getClass().getCanonicalName());
        }
        parent = new RemovableURLClassLoader(logger, urls, Plugin.class.getClassLoader());


    }


    public void setTempFolder() throws IOException {
        try {
            tempPluginDir = Files.createTempDirectory("flexicore").resolve(pluginDir.getFileName());
            tempPluginDir.toFile().deleteOnExit();
            logger.info("temp plugin folder is " + tempPluginDir.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileUtils.copyDirectory(this.pluginDir.toFile(), tempPluginDir.toFile());


    }

    public PluginLoader(Path pluginDir) {
        this.pluginDir = pluginDir;
    }

    /**
     * on thread start get acquainted with existing PI
     */
    @Override
    public void run() {

        try {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            pluginDir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
            scan(watcher);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "unable to start Watcher", e);
        }

    }


    @SuppressWarnings("unchecked")
    public <T extends Plugin> void loadInternal() {
        String packageName = PluginLoader.class.getPackage().getName();
        Reflections entitisReflection = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(packageName))
                .setScanners(new TypeAnnotationsScanner())
                .useParallelExecutor());


        Set<Class<?>> plugins = entitisReflection.getTypesAnnotatedWith(PluginInfo.class, true);
        List<Class<?>> toinstansiate = new ArrayList<>();
        Map<Class<?>, Integer> order = new HashMap<>();
        ModuleManifest manifest = getThisManifest();
        for (Class<?> plugin : plugins) {
            PluginInfo info = plugin.getAnnotation(PluginInfo.class);
            Class<T> type = isPlugin(plugin);
            // String path = "internal-" + Plugin.class.getCanonicalName();
            pluginService.addPlugin((Class<T>) plugin, type, manifest, info, true);
            if (info.autoInstansiate()) {
                toinstansiate.add(plugin);
                order.put(plugin, info.order());
            }

        }
        List<Class<?>> toInstansiateSorted = toinstansiate.parallelStream().sorted(Comparator.comparing(f -> order.getOrDefault(f, PluginInfo.DEFAULT_ORDER))).collect(Collectors.toList());
        for (Class<?> class1 : toInstansiateSorted) {
            Object p = pluginService.instansiate(class1, new HashMap<>());
            if (p instanceof HealthIndicator) {
                healthContributorRegistry.register(p.getClass().getSimpleName(), (HealthIndicator) p);
            }
        }


    }

    /**
     * Receives Watcher that was init to watch the plugin dir
     *
     * @param watcher
     */
    @SuppressWarnings("unchecked")
    private void scan(WatchService watcher) {
        WatchKey key;

        while (!stop) {
            try {
                key = watcher.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    // This key is registered only
                    // for ENTRY_CREATE events,
                    // but an OVERFLOW event can
                    // occur regardless if events
                    // are lost or discarded.
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path filename = ev.context();
                        File file = filename.toFile();
                        String name = file.getName();
                        String path = file.getAbsolutePath();
                        if (filter.accept(file, name)) {
                            Files.copy(filename, tempPluginDir);
                            ModuleManifest moduleManifest = getDependencies(file, logger, PluginType.Service);
                            ArrayList<Class<?>> jarLoaded = doLoad(name, null, false, moduleManifest);
                            Map<ModuleManifest, JarloadedClasses> jars = new HashMap<>();
                            jars.put(moduleManifest, new JarloadedClasses(jarLoaded, path));
                            if (jarLoaded != null) {
                                handleEntities(jars, false);
                            }

                        }
                    }

                    if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path filename = ev.context();
                        File file = filename.toFile();
                        String name = file.getName();

                        handleRemoval(name);

                    }

                    boolean valid = key.reset();
                    if (!valid) {
                        logger.log(Level.WARNING, "watcher key is invalid");
                        break;
                    }

                }
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "plugin Loader Interuppted", e);

            } catch (MalformedURLException e) {
                logger.log(Level.WARNING, "jar path is invalid", e);
            } catch (IOException e) {
                logger.log(Level.WARNING, "unable to close classLoader", e);
            }

        }

    }


    /**
     * Check if the class is a plug-in and has the Annotation {@link PluginInfo}
     * also handles dynamic REST resource discovery and the registration of new
     * operations.
     *
     * @param jars
     * @param onStart
     * @param <T>
     */
    public <T extends Plugin> void handleEntities(Map<ModuleManifest, JarloadedClasses> jars, boolean onStart) {
        SecurityContext securityContext = securityService.getAdminUserSecurityContext();
        boolean beanResolved = false;
        List<Class<?>> webSocketEndPoints = new ArrayList<>();
        List<Class<?>> restclasses = new ArrayList<>();
        List<Class<?>> providers = new ArrayList<>();

        List<Class<T>> autoInstanciateClasses = new ArrayList<>();
        Map<Class<T>, Integer> instansiateOrder = new HashMap<>();
        List<Class<?>> beansToCreate = new ArrayList<>();

        List<Class<?>> dbClasses = new ArrayList<>();
        Collection<Class<?>> crudTypes = new HashSet<>();
        Collection<Class<?>> crudAsyncTypes = new HashSet<>();

        for (Map.Entry<ModuleManifest, JarloadedClasses> entry : jars.entrySet()) {
            JarloadedClasses jar = entry.getValue();
            for (Class<?> c : jar.getLoaded()) {
                //  handleNoSQLClass(c,databases,databasesAsync,crudTypes,crudAsyncTypes,dbClasses);
                Class<T> type = isPlugin(c);
                if (type != null) {

                    if (c.isAnnotationPresent(PluginInfo.class) && !c.isInterface()) {
                        beansToCreate.add(c);
                        if (!beanResolved) {
                            // pluginService.registerBeansForPlugin(loadedEntities);
                            // //TODO: handle entitis for plugins
                            beanResolved = true;
                        }
                        PluginInfo pluginInfo = c.getAnnotation(PluginInfo.class);
                        if (pluginInfo.autoInstansiate()) {
                            autoInstanciateClasses.add((Class<T>) c);
                            instansiateOrder.put((Class<T>) c, pluginInfo.order());
                        }
                        loadPlugin(c, type, entry.getKey(), pluginInfo);
                        logger.fine("class added to pool: " + c.getCanonicalName() + " from jar " + jar.getPath()
                                + ", version " + pluginInfo.version() + " ,implementing " + type.getCanonicalName());
                    }


                    //get ignored methods to exclude from swagger doc
                  /*  List<Method> ignored = OpenAPIRESTService.getMethodsAnnotatedWith(c, JsonIgnore.class, logger);
                    List<Method> view = OpenAPIRESTService.getMethodsAnnotatedWith(c, JsonView.class, logger);
                    OpenAPIRESTService.addJsonIgnoreMethods(c, ignored);
                    OpenAPIRESTService.addJsonViewMethods(c, view);*/

                    if (c.isAnnotationPresent(javax.ws.rs.Path.class)) {
                        restclasses.add(c);

                    }

                    if (c.isAnnotationPresent(Provider.class)) {
                        providers.add(c);

                    }

                    if (c.isAnnotationPresent(ServerEndpoint.class)) {
                        webSocketEndPoints.add(c);
                    }
                    if (c.isAnnotationPresent(OperationsInside.class)) {
                        classScannerService.registerOperationsInclass(c); // Adds
                        // new
                        // operations
                        // found in
                        // the PI
                        // (for
                        // access
                        // control
                        // management)
                    }
                    if (c.isAnnotationPresent(InvokerInfo.class)) {
                        classScannerService.registerInvoker(c, securityContext);
                    }
                    if (c.isAnnotationPresent(TestsInside.class)) {

                        TestsRepository.put(c.getCanonicalName(), c);
                    }
                    if (c.isAnnotationPresent(OpenAPIDefinition.class)) {
                        classScannerService.addSwaggerTags(c, securityContext);
                    }


                }
            }
        }

        List<Bean<?>> beans = new ArrayList<>();
        List<AnnotatedType> annotatedTypes = new ArrayList<>();

       /* for (Class<?> class1 : dbClasses) {
            logger.fine("registering bean for class " + class1.getCanonicalName());
            pluginService.registerBean(class1);
        }*/

        ///registerNoSqlBeans(databases,databasesAsync,crudTypes,crudAsyncTypes);


        logger.fine("registering new beans....");

        for (Class<?> class1 : beansToCreate) {
            logger.fine("registering bean for class " + class1.getCanonicalName());
            pluginService.registerBean(class1);
        }

        logger.fine("creating instances for auto istanciated classes");
        List<Class<T>> instansiateSorted = autoInstanciateClasses.parallelStream().sorted(Comparator.comparing(f -> instansiateOrder.getOrDefault(f, PluginInfo.DEFAULT_ORDER))).collect(Collectors.toList());
        for (Class<T> class1 : instansiateSorted) {
            logger.fine("creating instance for " + class1.getCanonicalName());
            try {
                T t = pluginService.instansiate(class1, new HashMap<>());
                if (t instanceof HealthIndicator) {
                    healthContributorRegistry.register(t.getClass().getSimpleName(),(HealthIndicator) t);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "unable to instanciate: " + class1.getCanonicalName(), e);
            }

        }
        logger.fine("registering jax-rs providers");
        for (Class<?> provider : providers) {
            JaxRsActivator.addProvider(provider);
        }
       // requestContextController.activate();
        logger.fine("adding rest services...");
        for (Class<?> rest : restclasses) {
            logger.info("adding REST services from class: " + rest.getCanonicalName());
            try {
                Object o = pluginService.instansiate((Class<T>) rest, new HashMap<>());
                JaxRsActivator.add(rest);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "unable to instanciate REST Class: " + rest.getCanonicalName(), e);
            }
        }
        //requestContextController.deactivate();


        for (Class<?> webSocketEndPoint : webSocketEndPoints) {
            try {
                serverContainer.addEndpoint(webSocketEndPoint);
            } catch (DeploymentException e) {
                logger.log(Level.SEVERE, "unable to add endpoint", e);
            }
        }

    }


    /**
     * checks if a class is a plug-in
     *
     * @param c
     * @return
     */
    @SuppressWarnings("unchecked")
    private <T extends Plugin> Class<T> isPlugin(Class<?> c) {
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

    /**
     * loads a class as a plugin into {@link PluginRepository}
     *
     * @param c
     * @param type
     * @param moduleManifest
     * @param pluginInfo
     * @param <T>
     */
    @SuppressWarnings("unchecked")
    private <T extends Plugin> void loadPlugin(Class<?> c, Class<T> type, ModuleManifest moduleManifest, PluginInfo pluginInfo) {
        Class<T> plugin = (Class<T>) c;
        pluginService.addPlugin(plugin, type, moduleManifest, pluginInfo);

    }

    /**
     * loades a jar
     *
     * @param name
     * @param folder folder containing the jar
     * @throws MalformedURLException
     * @throws IOException
     */
    public ArrayList<Class<?>> doLoad(String name, String folder, boolean onStar, ModuleManifest moduleManifest)
            throws MalformedURLException, IOException {
        if (folder == null) {
            folder = tempPluginDir.toString();
        }


        String pathWithoutPrefixAndSufix = folder + Constants.SEPERATOR + name;
        pathWithoutPrefixAndSufix = pathWithoutPrefixAndSufix.replace("\\", "/");
        String s = "jar:" + "file:" + pathWithoutPrefixAndSufix + "!/";

        URL url = new URL(s);
        URL[] urls = {url};
        // in order to free resources loaded by this Jar,
        // we need to keep it on a list and release all of the dependencies and
        // the list of classes included in the Jar
        jarLoader = new GenericJarLoader(urls, tempPluginDir, parent);
        jarLoader.setLogger(logger);

        ArrayList<Class<?>> loaded = jarLoader.loadJar(name, moduleManifest);
        classLoaders.add(jarLoader);
        //jarLoader.close();
        return loaded;
    }

    /**
     * loads existing jars at pluginDir
     */
    public Map<ModuleManifest, JarloadedClasses> loadExisting() throws MissingDependencies {


        pluginService.loadModelListing();

        logger.info("loading plugins from: " + tempPluginDir.toFile().getAbsolutePath());
        Map<ModuleManifest, JarloadedClasses> loaded = loadPluginClasses();
        HandleAllJarsEntities(loaded, true);
        Initializator.setInit();
        logger.info("loading existing plugins - DONE");

        return loaded;

    }

    public Map<ModuleManifest, JarloadedClasses> loadPluginClasses() throws MissingDependencies {
        File[] jars = tempPluginDir.toFile().listFiles(filter);
        Map<ModuleManifest, JarloadedClasses> loaded = new HashMap<>();
        if (jars == null) {
            return loaded;
        }
        Arrays.sort(jars, Comparator.comparing(f -> f.getName()));
        List<JarPlugin> jarsByOrder = orderJars(jars);
        for (JarPlugin jarPlugin : jarsByOrder) {
            File file = jarPlugin.getFile();
            logger.info("attempting to load jar " + file.getAbsolutePath());
            try {
                ArrayList<Class<?>> jarLoaded = doLoad(file.getName(), file.getParentFile().getAbsolutePath(), true, jarPlugin.getModuleManifest());

                if (jarLoaded != null) {
                    logger.info("found " + jarLoaded.size() + " classes in jar " + file.getAbsolutePath());
                    loaded.put(jarPlugin.getModuleManifest(), new JarloadedClasses(jarLoaded, file.getAbsolutePath()));
                    for (Class<?> aClass : jarLoaded) {
                        InheritanceUtils.registerClass(aClass);

                    }

                } else {
                    logger.info("unable to load jar" + file.getAbsolutePath());
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "unable to load existing jar", e);
            }
        }
        return loaded;
    }

    private List<JarPlugin> orderJars(File[] jars) throws MissingDependencies {
        Map<Integer, ModuleManifest> dependenciesMap = new HashMap<>();
        List<File> ordered = new ArrayList<>();
        List<JarPlugin> orderedWithManifest = new ArrayList<>();
        for (int i = 0; i < jars.length; i++) {
            File jar = jars[i];
            ModuleManifest moduleManifest = getDependencies(jar, logger, PluginType.Service);

            dependenciesMap.put(i, moduleManifest);
            logger.info("manifest for " + jar.getAbsolutePath() + ": " + moduleManifest);
        }

        Map<Integer, ModuleManifest> missingDependenciesMap = new HashMap<>(dependenciesMap);


        for (int i = 0; i < jars.length; i++) {
            File jar = jars[i];
            if (ordered.contains(jar)) {
                continue;
            }
            ModuleManifest moduleManifest = dependenciesMap.get(i);
            if (moduleManifest.getRequires().isEmpty()) {
                missingDependenciesMap.remove(i);

                List<Integer> afterIndex = updateDependenciesMap(dependenciesMap, i, moduleManifest);
                for (Integer integer : afterIndex) {
                    missingDependenciesMap.remove(integer);
                }
                ordered.add(jar);
                orderedWithManifest.add(new JarPlugin(jar, moduleManifest));
                List<File> after = afterIndex.stream().map(j -> jars[j]).collect(Collectors.toList());
                List<JarPlugin> afterWithManifest = afterIndex.stream().map(j -> new JarPlugin(jars[j], dependenciesMap.get(j))).collect(Collectors.toList());
                ordered.addAll(after);
                orderedWithManifest.addAll(afterWithManifest);

            }
        }

        if (missingDependenciesMap.isEmpty()) {
            return orderedWithManifest;
        }
        //handle missing dependencies;
        String cycleString = getCycleString(missingDependenciesMap);
        if (cycleString != null && !cycleString.equals("[]")) {
            throw new MissingDependencies("Cyclic Dependencies: " + cycleString);
        }
        StringJoiner stringJoiner = new StringJoiner(",\n");
        for (Map.Entry<Integer, ModuleManifest> entry : missingDependenciesMap.entrySet()) {
            ModuleManifest manifest = entry.getValue();
            if (manifest.getRequires().isEmpty()) {
                continue;
            }
            int key = entry.getKey();
            String missingMessage = jars[key].getName() + " Missing Dependencies: " + manifest.getRequires().parallelStream().collect(Collectors.joining(","));

            stringJoiner.add(missingMessage);
        }
        String missing = stringJoiner.toString();
        throw new MissingDependencies("Missing Dependencies: " + missing);


    }

    private String getCycleString(Map<Integer, ModuleManifest> missingDependenciesMap) {
        Graph<String, DefaultEdge> g = new DefaultDirectedGraph<>(DefaultEdge.class);
        for (ModuleManifest manifest : missingDependenciesMap.values()) {
            for (String req : manifest.getRequires()) {
                for (String prov : manifest.getProvides()) {
                    g.addVertex(prov);
                    g.addVertex(req);
                    g.addEdge(prov, req);
                }
            }


        }
        CycleDetector<String, DefaultEdge> cycleDetector = new CycleDetector<>(g);
        Set<String> set = cycleDetector.findCycles();

        return set.toString();
    }

    private List<Integer> updateDependenciesMap(Map<Integer, ModuleManifest> dependenciesMap, Integer i, ModuleManifest moduleManifest) {
        List<Integer> toLoad = new ArrayList<>();
        Map<Integer, ModuleManifest> dependenciesForNextIteration = new HashMap<>(dependenciesMap);
        dependenciesForNextIteration.remove(i);
        for (Map.Entry<Integer, ModuleManifest> entry : dependenciesMap.entrySet()) {
            if (entry.getKey().equals(i)) {
                continue;
            }
            ModuleManifest man = entry.getValue();
            man.getRequires().removeAll(moduleManifest.getProvides());
            if (man.getRequires().isEmpty()) {
                toLoad.add(entry.getKey());
                dependenciesForNextIteration.remove(entry.getKey());
            }
        }

        List<Integer> add = new ArrayList<>();
        for (Integer integer : toLoad) {
            add.addAll(updateDependenciesMap(dependenciesForNextIteration, integer, dependenciesMap.get(integer)));
        }
        toLoad.addAll(add);
        return toLoad;

    }


    private ModuleManifest getThisManifest() {
        File classPath = new File(FlexiCore.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getParentFile().getParentFile().getParentFile();

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("flexicore-manifest.mf");
        Properties properties = new Properties();
        if (inputStream != null) {
            try {
                properties.load(inputStream);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "unable to load flexicore manifest for FLEXICORE", e);
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "unable to close inputstream", e);
                }

            }
        }

        String uuid = properties.getProperty("uuid", "unknown" + UUID.randomUUID());
        String version = properties.getProperty("version", "unknown" + UUID.randomUUID());
        return new ModuleManifest(uuid, version, classPath.getAbsolutePath(), classPath.getAbsolutePath(), new HashSet<>(), new HashSet<>(), PluginType.Core);

    }

    public static String getModuleDir(PluginType pluginType) {
        if (pluginType.equals(PluginType.Model)) {
            return Constants.ENTITIES_PATH;
        }
        if (pluginType.equals(PluginType.Service)) {
            return Constants.PLUGIN_PATH;
        }
        return null;
    }


    public static ModuleManifest getDependencies(File jar, Logger logger, PluginType pluginType) {
        String originalPath = getModuleDir(pluginType) + File.separator + jar.getName();

        ModuleManifest moduleManifest = new ModuleManifest(jar.getAbsolutePath(), originalPath, pluginType);
        try {
            JarFile jarFile = new JarFile(jar);
            JarEntry jarEntry = jarFile.getJarEntry("flexicore-manifest.mf");
            if (jarEntry == null) {
                logger.log(Level.SEVERE, "missing manifest File For: " + jar.getAbsolutePath());
                return moduleManifest;
            }
            InputStream inputStream = jarFile.getInputStream(jarEntry);
            Properties properties = new Properties();
            properties.load(inputStream);
            HashSet<String> requires = Stream.of(properties.getProperty("requires", "").split(",")).filter(s -> s != null && !s.isEmpty()).map(f -> f.trim()).collect(Collectors.toCollection(HashSet::new));
            HashSet<String> basicProvides = Stream.of(properties.getProperty("provides", "").split(",")).filter(s -> s != null && !s.isEmpty()).map(f -> f.trim()).collect(Collectors.toCollection(HashSet::new));
            String uuid = properties.getProperty("uuid", "unknown" + UUID.randomUUID());
            String version = properties.getProperty("version", "unknown" + UUID.randomUUID());

            HashSet<String> provides = jarFile.stream().filter(f -> isClassProvided(f.getName().replaceAll("/", "."), basicProvides)).map(f -> StringUtils.removeEnd(f.getName().replaceAll("/", "."), ".class")).collect(Collectors.toCollection(HashSet::new));
            moduleManifest.set(uuid, version, jar.getAbsolutePath(), originalPath, requires, provides, pluginType);


        } catch (IOException e) {
            logger.log(Level.SEVERE, "unable to get dependencies", e);
        }
        return moduleManifest;
    }


    public static boolean isClassProvided(String fullName, HashSet<String> basicProvides) {
        for (String basicProvide : basicProvides) {
            if (fullName.contains(basicProvide) && !fullName.endsWith(".")) {
                return true;
            }
        }
        return false;
    }

    private File getEntitiesFolder(String entitiesFolder) {
        File original = new File(entitiesFolder);
        Path temp = tempPluginDir.getParent().resolve(original.getName());
        try {
            FileUtils.copyDirectory(original, temp.toFile());

            return temp.toFile();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "unable to create temp folder", e);
        }
        return null;

    }


    private boolean loadEntities() {
        File entitiesFolder = getEntitiesFolder(Constants.ENTITIES_PATH);
        if (entitiesFolder == null) {
            return false;
        }
        if (entitiesFolder.exists()) {
            logger.info("loading entities from: " + Constants.ENTITIES_PATH);
            File[] jars = entitiesFolder.listFiles(filter);
            boolean reload = false;
            boolean force = pluginService.isUpdateForced();
            for (File file : jars != null ? jars : new File[0]) {
                logger.info("attempting to load jar " + file.getAbsolutePath());
                try {
                    reload = pluginService.addJarToClassPath(file, force) || reload;
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "unable to add entities Jar", e);
                }

            }
            pluginService.loadModelListing();
            if (force) {
                pluginService.deleteForceFile();
            }

            if (reload) {

                // wildflyService.reload(Constants.COOLDOWN_TIME);

            }
            return reload;
        } else {
            logger.warning("entites folder: " + Constants.ENTITIES_PATH + " does not exists");
            return false;
        }

    }

    private void HandleAllJarsEntities(Map<ModuleManifest, JarloadedClasses> list, boolean onStart) {

        handleEntities(list, onStart);

    }

    public void closeClassLoaders() {
        while (!classLoaders.isEmpty()) {
            URLClassLoader urlClassLoader = classLoaders.remove();
            try {
                urlClassLoader.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "unable to close classloader", e);
            }
        }
    }


    public class JarloadedClasses {
        private ArrayList<Class<?>> loaded;
        private String path;

        public JarloadedClasses(ArrayList<Class<?>> loaded, String path) {
            super();
            this.loaded = loaded;
            this.path = path;
        }

        public ArrayList<Class<?>> getLoaded() {
            return loaded;
        }

        public String getPath() {
            return path;
        }

    }

    /**
     * handles removal of jar
     *
     * @param path of jar
     */
    private void handleRemoval(String path) {
        //pluginService.removePlugins(path);
    }

    public void setPluginDir(Path pluginDir) {
        this.pluginDir = pluginDir;
    }


    public ServerContainer getServerContainer() {
        return serverContainer;
    }

    public void setServerContainer(ServerContainer serverContainer) {
        this.serverContainer = serverContainer;
    }
}
