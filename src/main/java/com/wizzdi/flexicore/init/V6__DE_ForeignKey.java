package com.wizzdi.flexicore.init;

import com.wizzdi.flexicore.boot.base.interfaces.Plugin;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.pf4j.Extension;
import org.springframework.stereotype.Component;

import java.sql.Statement;

@Extension
@Component
public class V6__DE_ForeignKey extends BaseJavaMigration implements Plugin {

	@Override
	public void migrate(Context context) throws Exception {
		try (Statement select = context.getConnection().createStatement()) {
			select.execute("alter table servicecanonicalname drop constraint fk_servicecanonicalname_dynamicexecution_id");
			select.execute("alter table servicecanonicalname drop column dynamicexecution_id");


		}
	}
}
