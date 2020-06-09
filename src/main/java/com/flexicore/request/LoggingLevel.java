package com.flexicore.request;

import java.util.logging.Level;

public enum LoggingLevel {
    FINEST(Level.FINEST),FINER(Level.FINER),FINE(Level.FINE),INFO(Level.INFO),WARNING(Level.WARNING),SEVERE(Level.SEVERE);

    private Level level;
    LoggingLevel(Level level) {
        this.level=level;
    }

    public Level getLevel() {
        return level;
    }

}
