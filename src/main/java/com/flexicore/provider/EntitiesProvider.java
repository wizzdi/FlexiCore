package com.flexicore.provider;

import com.flexicore.model.Baseclass;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import javax.persistence.Entity;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class EntitiesProvider {

    private static final Logger logger= LoggerFactory.getLogger(EntitiesProvider.class);
    @Value("${flexicore.entities}")
    private String entitiesPath;

    /**
     * this will return all entities in flexicore and in ${flexicore.entities} path
     * we make sure to limit the search so this wont cause the loading of unwanted classes with that loader
     * in fact if we did do that several app critical classes(direct FC dependencies) will be loaded by the Reflection library
     * causing ClassNotFound exceptions and making meta model classes fields types to be null
     * @return
     */

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public EntitiesHolder getEntities() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        List<URL> entitiesJarsUrls = getEntitiesJarsUrls();
        entitiesJarsUrls.add(getFCLocation());
        ConfigurationBuilder configuration = ConfigurationBuilder.build()
                .addClassLoader(classLoader)
                .setUrls(entitiesJarsUrls);
        Reflections reflections = new Reflections(configuration);
        Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(Entity.class);
        return new EntitiesHolder(typesAnnotatedWith);
    }

    private URL getFCLocation() {
        return Baseclass.class.getProtectionDomain().getCodeSource().getLocation();

    }

    private List<URL> getEntitiesJarsUrls() {
        File file=new File(entitiesPath);
        if(file.exists() && file.isDirectory()){
            File[] jars=file.listFiles();
            if(jars!=null){
                return Stream.of(jars).filter(f->f.getName().endsWith(".jar")).map(this::getJarURL).filter(Objects::nonNull).collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }

    private URL getJarURL(File f) {
        try {
            return new URL(new URI("jar", f.toURI().toString(),  null).toString()+"!/");
        } catch (MalformedURLException | URISyntaxException e) {
            logger.error("failed getting jar url for file"+f.getAbsolutePath());
        }
        return null;
    }
}
