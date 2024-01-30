package com.wizzdi.flexicore.init;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.List;

@Component
public class V9__BaseToSecurity extends BaseJavaMigration {

    private static final Logger logger = LoggerFactory.getLogger(V9__BaseToSecurity.class);

    private static final List<TypeNameMigration> typeMigrations=List.of(
            new TypeNameMigration("Operation","SecurityOperation"),
            new TypeNameMigration("Tenant","SecurityTenant")
    );

    @Override
    public void migrate(Context context) throws Exception {

        Connection connection = context.getConnection();
        Savepoint v2_0 = connection.setSavepoint("v2_0");
        try (Statement select = context.getConnection().createStatement()) {
            for (TypeNameMigration typeMigration : typeMigrations) {
                logger.info("Starting Migration of "+typeMigration.oldName);
                String sql = "update baseclass set dtype='"+typeMigration.newName+"' where dtype='"+typeMigration.oldName+"'";
                logger.info("executing SQL: " + sql);
                int updatedEntries = select.executeUpdate(sql);
            }

        }


    }

    record TypeNameMigration(String oldName, String newName) {
    }



}