package com.wizzdi.flexicore.init;

import java.util.Set;

public class FlywayObjectHolder {
    private Set<String> classNames;

    public FlywayObjectHolder(Set<String> classNames) {
        this.classNames = classNames;
    }

    public Set<String> getClassNames() {
        return classNames;
    }

    public <T extends FlywayObjectHolder> T setClassNames(Set<String> classNames) {
        this.classNames = classNames;
        return (T) this;
    }
}
