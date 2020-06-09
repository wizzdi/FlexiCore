package com.flexicore.request;

public class LogEntry {

    private LoggingLevel level;
    private String message;

    public LoggingLevel getLevel() {
        return level;
    }

    public <T extends LogEntry> T setLevel(LoggingLevel level) {
        this.level = level;
        return (T) this;
    }

    public String getMessage() {
        return message;
    }

    public <T extends LogEntry> T setMessage(String message) {
        this.message = message;
        return (T) this;
    }
}
