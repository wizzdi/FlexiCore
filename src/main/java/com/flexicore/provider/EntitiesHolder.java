package com.flexicore.provider;

import java.util.Set;

public class EntitiesHolder {

    private final Set<Class<?>> entities;

    public EntitiesHolder(Set<Class<?>> entities) {
        this.entities = entities;
    }

    public Set<Class<?>> getEntities() {
        return entities;
    }
}
