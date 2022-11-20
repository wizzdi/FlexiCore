package com.wizzdi.flexicore.init;

import com.flexicore.model.Baseclass;
import com.wizzdi.flexicore.file.model.FileResource;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.*;
import java.time.ZoneOffset;

@Component
@Transactional
public class V7__FileResourceMigration extends BaseJavaMigration {

	private static final Logger logger= LoggerFactory.getLogger(V7__FileResourceMigration.class);
	@PersistenceContext
	private EntityManager em;

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		Savepoint v7_0 = connection.setSavepoint("V7_0");
		try (Statement select = context.getConnection().createStatement()) {

			ResultSet count = select.executeQuery("select count(*) as totalRows from baseclass where dtype='FileResource'");
			count.next();
			int totalRows = count.getInt("totalRows");
			logger.info("Starting File Resource Migration of "+totalRows +" FileResources");
			int updatedEntries = select.executeUpdate("insert into  fileresource(id,name,description,md5,fileoffset,actualFilename,originalFileName,done,path,dateTaken,nonDownloadable,keepUntil,onlyFrom,softDelete,dtype,security_id) select id,name,description,md5,fileoffset,actualFilename,originalFileName,done,path,dateTaken,nonDownloadable,keepUntil,onlyFrom,softDelete,dtype,id from baseclass where dtype='FileResource'");
			select.executeUpdate("update baseclass set dtype='Baseclass',clazz_id='' where dtype='FileResource'");

			Savepoint v7_1 = connection.setSavepoint("V7_1");
			try (Statement statement = connection.createStatement()) {

				statement.execute("alter table baseclass drop constraint fk_baseclass_icon_id");
				statement.execute("Alter Table baseclass Add Constraint fk_baseclass_icon_id Foreign Key (icon_id) References fileresource(id)");

			}
			catch (SQLException e){
				logger.warn("failed recreating fk_baseclass_icon_id constraint",e);
				connection.rollback(v7_1);
			}

			Savepoint v7_2 = connection.setSavepoint("V7_2");
			try (Statement statement = connection.createStatement()) {
				statement.execute("alter table baseclass drop constraint fk_baseclass_fileresource_id");
				statement.execute("Alter Table baseclass Add Constraint fk_baseclass_fileresource_id Foreign Key (fileresource_id) References fileresource(id)");

			}
			catch (SQLException e){
				logger.warn("failed recreating fk_baseclass_fileresource_id constraint",e);

				connection.rollback(v7_2);
			}


		}
		catch (SQLException e){
			logger.warn("failed getting filersource count",e);
			connection.rollback(v7_0);
		}




	}

}
