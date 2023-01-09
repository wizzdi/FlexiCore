package com.wizzdi.flexicore.init;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.migration.JavaMigration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

@Configuration
public class FlywayConfiguration {

    @Value("${wizzdi.flyway.enabled:true}")
    private boolean flywayEnabled;

    @Bean
    public FlywayMigrationsHolder flywayMigrationsHolder(DataSource dataSource, ObjectProvider<JavaMigration> javaMigrationsProvider, EntityManagerFactory entityManagerFactory){
        if(!flywayEnabled){
            return new FlywayMigrationsHolder(Collections.emptySet());
        }
        JavaMigration[] javaMigrations = javaMigrationsProvider.stream().toArray(JavaMigration[]::new);
        Flyway flyway = Flyway.configure().javaMigrations(javaMigrations).baselineOnMigrate(true).dataSource(dataSource).load();
        flyway.migrate();
        return new FlywayMigrationsHolder(Arrays.stream(javaMigrations).map(f->f.getClass().getCanonicalName()).collect(Collectors.toSet()));

    }
}