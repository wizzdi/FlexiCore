package com.flexicore.response;

import java.util.Set;

public class MassDeleteResponse {
    private Set<String> deletedIds;

    public Set<String> getDeletedIds() {
        return deletedIds;
    }

    public <T extends MassDeleteResponse> T setDeletedIds(Set<String> deletedIds) {
        this.deletedIds = deletedIds;
        return (T) this;
    }
}
