package com.wizzdi.flexicore.init;

import com.flexicore.model.User;
import com.wizzdi.flexicore.security.migrations.FlexiCoreV4ToV5Migration;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.Savepoint;
import java.sql.Statement;

@Component

public class V9__FCV5Migration extends BaseJavaMigration {

    private static final Logger logger = LoggerFactory.getLogger(V9__FCV5Migration.class);


    @Override
    public void migrate(Context context) throws Exception {

        Connection connection = context.getConnection();
        Savepoint v3_0 = connection.setSavepoint("v3_0");
        try (Statement select = context.getConnection().createStatement()) {
            FlexiCoreV4ToV5Migration.migrateToFCV5(select, User.class);

        }


    }



}
