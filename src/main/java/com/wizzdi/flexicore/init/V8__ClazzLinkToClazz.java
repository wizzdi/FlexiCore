package com.wizzdi.flexicore.init;

import com.wizzdi.flexicore.security.migrations.ClazzLinkMigration;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.Savepoint;
import java.sql.Statement;

@Component
public class V8__ClazzLinkToClazz extends BaseJavaMigration {

    private static final Logger logger = LoggerFactory.getLogger(V8__ClazzLinkToClazz.class);

    @Override
    public void migrate(Context context) throws Exception {

        Connection connection = context.getConnection();
        Savepoint v2_0 = connection.setSavepoint("v2_0");
        try (Statement select = context.getConnection().createStatement()) {

            ClazzLinkMigration.migrateClazzLink(select);


        }


    }




}