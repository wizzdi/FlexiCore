package com.flexicore.service.internal.plugins.invokers;

import com.flexicore.annotations.plugins.PluginInfo;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.interfaces.dynamic.InvokerInfo;
import com.flexicore.interfaces.dynamic.InvokerMethodInfo;
import com.flexicore.interfaces.dynamic.ListingInvoker;
import com.flexicore.model.auditing.AuditingEvent;
import com.flexicore.request.AuditingFilter;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.AuditingService;

import javax.inject.Inject;

@PluginInfo(version = 1)
@InvokerInfo
public class AuditingInvoker implements ListingInvoker<AuditingEvent, AuditingFilter> {

    @Inject
    private AuditingService auditingEventService;

    @Override
    @InvokerMethodInfo(displayName = "listAllAuditingEvents",description = "lists all auditingEvents")
    public PaginationResponse<AuditingEvent> listAll(AuditingFilter filter, SecurityContext securityContext) {
        auditingEventService.validate(filter,securityContext);
        return auditingEventService.getAllAuditingEvents(filter,securityContext);
    }

    @Override
    public Class<AuditingFilter> getFilterClass() {
        return AuditingFilter.class;
    }

    @Override
    public Class<?> getHandlingClass() {
        return AuditingEvent.class;
    }
}
