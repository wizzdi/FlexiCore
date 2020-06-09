package com.flexicore.jobs.messages;


public class JobWSHelloMessage extends JobMessage{
   private String sessionId;

    public String getSessionId() {
        return sessionId;
    }

    public JobWSHelloMessage setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    @Override
    public JobWSHelloMessage setId(String id) {
        return (JobWSHelloMessage) super.setId(id);
    }
}
