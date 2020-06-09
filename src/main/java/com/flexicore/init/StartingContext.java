package com.flexicore.init;

import com.flexicore.model.Clazz;

import java.util.List;

public class StartingContext {

    private final List<Clazz> clazzes;

    public StartingContext(List<Clazz> clazzes) {
        this.clazzes = clazzes;
    }

    public List<Clazz> getClazzes() {
        return clazzes;
    }
}
