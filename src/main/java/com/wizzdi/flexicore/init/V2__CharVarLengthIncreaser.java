package com.wizzdi.flexicore.init;

import com.wizzdi.flexicore.boot.base.interfaces.Plugin;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.pf4j.Extension;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.Statement;

@Extension
@Component
public class V2__CharVarLengthIncreaser extends BaseJavaMigration implements Plugin {

	@Override
	public void migrate(Context context) throws Exception {
		try (Statement select = context.getConnection().createStatement()) {
			try (ResultSet rows = select.executeQuery("select column_name from information_schema.columns where table_name ='baseclass' and data_type='character varying'\n" +
					"and character_maximum_length=22")) {
				while (rows.next()) {
					String name= rows.getString("column_name");
					try (Statement update = context.getConnection().createStatement()) {
						update.execute("alter table baseclass alter column "+name+" type varchar(255)");
					}
				}

			}
		}
	}
}
