package com.flexicore.provider;

import com.flexicore.converters.JsonConverter;
import com.flexicore.model.Baseclass;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.persistence.Entity;
import javax.sql.DataSource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableTransactionManagement
public class EclipseLinkJpaConfiguration extends JpaBaseConfiguration {


    private Logger logger=LoggerFactory.getLogger(EclipseLinkJpaConfiguration.class);

    @Value("${eclipselink.ddl-generation}")
    private String ddlGeneration;


    protected EclipseLinkJpaConfiguration(DataSource dataSource, JpaProperties properties, ObjectProvider<JtaTransactionManager> jtaTransactionManager) {
        super(dataSource, properties, jtaTransactionManager);



    }

    @Bean
    @Primary
    public PlatformTransactionManager jpaTransactionManager(ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        TransactionManagerCustomizers managerCustomizersIfAvailable = transactionManagerCustomizers.getIfAvailable();
        if (managerCustomizersIfAvailable != null) {
            managerCustomizersIfAvailable.customize(transactionManager);
        }

        return transactionManager;
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(final EntityManagerFactoryBuilder builder, @Autowired DataSource dataSource,@Autowired EntitiesHolder entitiesHolder) throws ClassNotFoundException, MalformedURLException {
        Set<Class<?>> entities = new HashSet<>(entitiesHolder.getEntities());
        entities.add(JsonConverter.class);
        logger.debug("Discovered Entities: "+entities.stream().map(f->f.getCanonicalName()).collect(Collectors.joining(System.lineSeparator())));
        Class<?>[] entitiesArr=new Class<?>[entities.size()];
        entities.toArray(entitiesArr);
        EntityManagerFactoryBuilder.Builder primary = builder.dataSource(dataSource)
                .packages(entitiesArr)
                .persistenceUnit("primary")
                .properties(getVendorProperties())
                .jta(true);
        final LocalContainerEntityManagerFactoryBean ret = primary.build();

        return ret;
    }



    @Override
    protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
        return new EclipseLinkJpaVendorAdapter();
    }

    @Override
    protected Map<String, Object> getVendorProperties() {
        Map<String,Object> props=new HashMap<>();
        props.put("eclipselink.target-database","PostgreSQL");
        props.put("eclipselink.weaving", "false");
        props.put("javax.persistence.schema-generation.create-database-schemas", "true");
        props.put("eclipselink.ddl-generation.output-mode", "database");
        props.put("eclipselink.ddl-generation", ddlGeneration);
        props.put("eclipselink.logging.level", "FINE");
        props.put("eclipselink.logging.level.sql", "FINE");
        props.put("eclipselink.logging.parameters", "true");
        props.put("eclipselink.exclude-eclipselink-orm", "true");
        props.put("eclipselink.jdbc.batch-writing", "JDBC");
        props.put("eclipselink.jdbc.batch-writing.size", "1000");
        props.put("eclipselink.jdbc.sql-cast","true");



        return props;
    }




}
