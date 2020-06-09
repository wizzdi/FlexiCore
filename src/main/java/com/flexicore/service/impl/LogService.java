package com.flexicore.service.impl;

import com.flexicore.interfaces.FlexiCoreService;
import com.flexicore.request.LogCreate;
import com.flexicore.request.LogEntry;
import com.flexicore.security.SecurityContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.ws.rs.BadRequestException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

@Primary
@Component
public class LogService implements FlexiCoreService {


    public void log(LogCreate logCreate, SecurityContext securityContext) {
        Logger logger=LogManager.getLogManager().getLogger(logCreate.getLoggerName());
        for (LogEntry logEntry : logCreate.getLogEntryList()) {
            logger.log(logEntry.getLevel().getLevel(),logEntry.getMessage());
        }

    }

    public void validate(LogCreate logCreate, SecurityContext securityContext) {
        if(logCreate.getLoggerName()==null){
            throw new BadRequestException("logger name must be provided");
        }
    }
}
