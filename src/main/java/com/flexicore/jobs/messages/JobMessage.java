package com.flexicore.jobs.messages;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.flexicore.data.jsoncontainers.CrossLoaderResolver;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,property = "type")
@JsonTypeIdResolver(CrossLoaderResolver.class)
public class JobMessage {
    private String id;

    public String getId() {
        return id;
    }

    public JobMessage setId(String id) {
        this.id = id;
        return this;
    }
}
