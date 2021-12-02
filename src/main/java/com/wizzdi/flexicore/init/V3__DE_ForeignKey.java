package com.wizzdi.flexicore.init;

import com.wizzdi.flexicore.boot.base.interfaces.Plugin;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.pf4j.Extension;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.Statement;

@Extension
@Component
public class V3__DE_ForeignKey extends BaseJavaMigration implements Plugin {

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection= context.getConnection();
		Savepoint before_v3_1 = connection.setSavepoint("before V3_1");
		try (Statement select = connection.createStatement()) {
				select.execute("alter table baseclass drop constraint fk_baseclass_dynamicexecution_id");

		}
		catch (Throwable ignored){
			connection.rollback(before_v3_1);
		}
		Savepoint before_v3_2 = connection.setSavepoint("before V3_2");

		try (Statement select = context.getConnection().createStatement()) {
			select.execute("alter table baseclass drop column dynamicexecution_id");

		}
		catch (Throwable ignored){
			connection.rollback(before_v3_2);

		}


	}
}
