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
import java.util.List;
import java.util.Set;

@Component

public class V10__FCV5Migration extends BaseJavaMigration {

    private static final Logger logger = LoggerFactory.getLogger(V10__FCV5Migration.class);


    @Override
    public void migrate(Context context) throws Exception {


        Connection connection = context.getConnection();
        Savepoint v3_0 = connection.setSavepoint("v3_0");
        try (Statement select = context.getConnection().createStatement()) {
            if(MigrationUtils.getFields(select,Set.of("clazz")).isEmpty()){
                return;
            }
            FlexiCoreV4ToV5Migration.migrateToFCV5(select, new FlexiCoreV4ToV5Migration.ExternalTypeMigration( List.of(new FlexiCoreV4ToV5Migration.FieldMigration("surName","lastName")),User.class));

        }


    }


}
