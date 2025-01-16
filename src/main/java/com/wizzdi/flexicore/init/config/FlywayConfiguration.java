package com.wizzdi.flexicore.init.config;

import com.flexicore.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.JavaMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // Ensure the highest priority

public class FlywayConfiguration implements InitializingBean {
    private static final Logger logger= LoggerFactory.getLogger(FlywayConfiguration.class);

    @Value("${flexicore.flyway.enable:true}")
    private boolean flywayEnabled;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private ObjectProvider<JavaMigration> javaMigrationsProvider;
    @PersistenceContext
    private EntityManager em;

    @Value("${flexicore.flyway.target:#{null}}")
    private String targetVersion;



    @Override
    public void afterPropertiesSet() throws Exception {
        if(!flywayEnabled){
            return;
        }
        triggerJPADDL();
        JavaMigration[] javaMigrations = javaMigrationsProvider.stream().toArray(JavaMigration[]::new);
        FluentConfiguration fluentConfiguration = Flyway.configure().javaMigrations(javaMigrations).baselineOnMigrate(true).dataSource(dataSource);
        if(targetVersion!=null){
            fluentConfiguration=fluentConfiguration.target(targetVersion);
        }
        Flyway flyway = fluentConfiguration.load();
        flyway.migrate();


    }

    private void triggerJPADDL() {
        try {
            User test = em.find(User.class, "test");
        }
        catch (Throwable e){
           logger.error("failed forcing schema creation");
        }
    }
}
