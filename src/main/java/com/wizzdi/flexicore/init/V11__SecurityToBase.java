package com.wizzdi.flexicore.init;

import com.wizzdi.flexicore.security.migrations.FlexiCoreV5ToV7Migration;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

@Component
public class V11__SecurityToBase extends BaseJavaMigration {

    private static final Logger logger = LoggerFactory.getLogger(V11__SecurityToBase.class);


    @Override
    public void migrate(Context context) throws Exception {

        Connection connection = context.getConnection();
        Savepoint v2_0 = connection.setSavepoint("v11_0");
        try (Statement select = context.getConnection().createStatement()) {
            if(MigrationUtils.getFields(select, Collections.singleton("baseclass")).isEmpty()){
                return;
            }
            FlexiCoreV5ToV7Migration.migrateToFCV7(select);


        }


    }


}
