package com.wizzdi.flexicore.init.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.migration.JavaMigration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

@Configuration
public class FlywayConfiguration {

    @Value("${flexicore.flyway.enable:true}")
    private boolean enabled;

    @Bean
    public FlywayObjectHolder flywayObjectHolder(DataSource dataSource, ObjectProvider<JavaMigration> javaMigrationsProvider, EntityManagerFactory entityManagerFactory) {
        if(!enabled){
            return new FlywayObjectHolder(Collections.emptySet());
        }
        JavaMigration[] javaMigrations = javaMigrationsProvider.stream().toArray(JavaMigration[]::new);
        Flyway flyway = Flyway.configure().javaMigrations(javaMigrations).baselineOnMigrate(true).dataSource(dataSource).load();
        flyway.migrate();
        return new FlywayObjectHolder(Arrays.stream(javaMigrations).map(f->f.getClass().getCanonicalName()).collect(Collectors.toSet()));
    }
}