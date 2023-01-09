package com.wizzdi.flexicore.init;

import java.util.Set;

public class FlywayMigrationsHolder {

    private final Set<String> migrations;

    public FlywayMigrationsHolder(Set<String> migrations) {
        this.migrations = migrations;
    }

    public Set<String> getMigrations() {
        return migrations;
    }
}
