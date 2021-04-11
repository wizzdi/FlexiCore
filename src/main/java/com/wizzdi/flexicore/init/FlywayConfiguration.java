package com.wizzdi.flexicore.init;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.migration.JavaMigration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfiguration {

    @Autowired
    public FlywayConfiguration(DataSource dataSource, ObjectProvider<JavaMigration> javaMigrationsProvider) {
        JavaMigration[] javaMigrations = javaMigrationsProvider.stream().toArray(JavaMigration[]::new);
        Flyway flyway = Flyway.configure().javaMigrations(javaMigrations).baselineOnMigrate(true).dataSource(dataSource).load();
        flyway.migrate();
    }
}