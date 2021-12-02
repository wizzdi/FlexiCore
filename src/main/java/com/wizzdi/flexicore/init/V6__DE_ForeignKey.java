package com.wizzdi.flexicore.init;

import com.wizzdi.flexicore.boot.base.interfaces.Plugin;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.pf4j.Extension;
import org.springframework.stereotype.Component;

import java.sql.*;

@Extension
@Component
public class V6__DE_ForeignKey extends BaseJavaMigration implements Plugin {

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		Savepoint v6_1 = connection.setSavepoint("V6_1");
		try (Statement select = connection.createStatement()) {

				select.execute("alter table servicecanonicalname drop constraint fk_servicecanonicalname_dynamicexecution_id");
		}
		catch (SQLException e){
			connection.rollback(v6_1);
		}

		Savepoint v6_2 = connection.setSavepoint("V6_2");
		try (Statement select = connection.createStatement()) {
			select.execute("alter table servicecanonicalname drop column dynamicexecution_id");
		}
		catch (SQLException e){
			connection.rollback(v6_2);
		}


	}
}
