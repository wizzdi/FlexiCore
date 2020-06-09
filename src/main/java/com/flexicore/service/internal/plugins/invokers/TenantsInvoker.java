package com.flexicore.service.internal.plugins.invokers;

import com.flexicore.annotations.plugins.PluginInfo;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.interfaces.dynamic.InvokerInfo;
import com.flexicore.interfaces.dynamic.InvokerMethodInfo;
import com.flexicore.interfaces.dynamic.ListingInvoker;
import com.flexicore.model.Tenant;
import com.flexicore.request.TenantFilter;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.TenantService;

import org.springframework.beans.factory.annotation.Autowired;

@PluginInfo(version = 1)
@InvokerInfo
public class TenantsInvoker implements ListingInvoker<Tenant, TenantFilter> {

    @Autowired
    private TenantService tenantService;

    @Override
    @InvokerMethodInfo(displayName = "listAllTenants",description = "lists all tenants")
    public PaginationResponse<Tenant> listAll(TenantFilter filter, SecurityContext securityContext) {
        return tenantService.getTenants(filter,securityContext);
    }

    @Override
    public Class<TenantFilter> getFilterClass() {
        return TenantFilter.class;
    }

    @Override
    public Class<?> getHandlingClass() {
        return Tenant.class;
    }
}
