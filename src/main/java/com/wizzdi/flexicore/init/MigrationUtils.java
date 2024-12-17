package com.wizzdi.flexicore.init;

import com.flexicore.model.Baseclass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MigrationUtils {
    private final static Logger logger = LoggerFactory.getLogger(MigrationUtils.class);

    public static Map<String, Set<String>> getFields(Statement select, Set<String> tableName) throws SQLException {

        String sql = "select table_name,column_name from information_schema.columns where table_name in (%s)".formatted(tableName.stream().map(f -> "'" + f + "'").collect(Collectors.joining(",")));
        logger.info("getting fields for tables SQL: {}", sql);
        ResultSet resultSet = select.executeQuery(sql);
        Map<String, Set<String>> tables = new HashMap<>();
        while (resultSet.next()) {
            Set<String> fields = tables.computeIfAbsent(resultSet.getString(1), f -> new HashSet<>());
            fields.add(resultSet.getString(2).toLowerCase());
        }
        return tables;


    }
}
