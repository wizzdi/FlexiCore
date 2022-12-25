package com.wizzdi.flexicore.init;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.migration.JavaMigration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
public class FlywayConfiguration {

    @Bean
    public FlywayObjectHolder flywayObjectHolder(DataSource dataSource, ObjectProvider<JavaMigration> javaMigrationsProvider, EntityManagerFactory entityManagerFactory) {
        JavaMigration[] javaMigrations = javaMigrationsProvider.stream().toArray(JavaMigration[]::new);
        Flyway flyway = Flyway.configure().javaMigrations(javaMigrations).baselineOnMigrate(true).dataSource(dataSource).load();
        flyway.migrate();
        return new FlywayObjectHolder(Arrays.stream(javaMigrations).map(f->f.getClass().getCanonicalName()).collect(Collectors.toSet()));
    }
}