package com.flexicore.jobs.messages;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.flexicore.data.jsoncontainers.CrossLoaderResolver;


public class JobPhaseChangedMessage extends JobMessage{
    private String oldPhase;
    private String newPhase;

    public String getOldPhase() {
        return oldPhase;
    }

    public JobPhaseChangedMessage setOldPhase(String oldPhase) {
        this.oldPhase = oldPhase;
        return this;
    }

    public String getNewPhase() {
        return newPhase;
    }

    public JobPhaseChangedMessage setNewPhase(String newPhase) {
        this.newPhase = newPhase;
        return this;
    }

    @Override
    public JobPhaseChangedMessage setId(String id) {
        return (JobPhaseChangedMessage) super.setId(id);
    }
}
