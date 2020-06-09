package com.flexicore.request;

import java.util.List;

public class LogCreate {

    private String loggerName;
    private List<LogEntry> logEntryList;

    public List<LogEntry> getLogEntryList() {
        return logEntryList;
    }

    public <T extends LogCreate> T setLogEntryList(List<LogEntry> logEntryList) {
        this.logEntryList = logEntryList;
        return (T) this;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public <T extends LogCreate> T setLoggerName(String loggerName) {
        this.loggerName = loggerName;
        return (T) this;
    }
}
