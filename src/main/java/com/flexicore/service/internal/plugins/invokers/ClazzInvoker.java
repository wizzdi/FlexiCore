package com.flexicore.service.internal.plugins.invokers;

import com.flexicore.annotations.plugins.PluginInfo;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.interfaces.dynamic.InvokerInfo;
import com.flexicore.interfaces.dynamic.InvokerMethodInfo;
import com.flexicore.interfaces.dynamic.ListingInvoker;
import com.flexicore.model.Clazz;
import com.flexicore.request.ClazzFilter;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.ClazzService;

import org.springframework.beans.factory.annotation.Autowired;

@PluginInfo(version = 1)
@InvokerInfo
public class ClazzInvoker implements ListingInvoker<Clazz, ClazzFilter> {

    @Autowired
    private ClazzService clazzService;

    @Override
    @InvokerMethodInfo(displayName = "listAllClazzes",description = "lists all Clazzes")
    public PaginationResponse<Clazz> listAll(ClazzFilter filter, SecurityContext securityContext) {
        return clazzService.getAllClazz(filter,securityContext);
    }

    @Override
    public Class<ClazzFilter> getFilterClass() {
        return ClazzFilter.class;
    }

    @Override
    public Class<?> getHandlingClass() {
        return Clazz.class;
    }
}
