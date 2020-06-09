package com.flexicore.service.internal.plugins.invokers;

import com.flexicore.annotations.plugins.PluginInfo;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.interfaces.dynamic.InvokerInfo;
import com.flexicore.interfaces.dynamic.InvokerMethodInfo;
import com.flexicore.interfaces.dynamic.ListingInvoker;
import com.flexicore.model.Operation;
import com.flexicore.request.OperationFiltering;
import com.flexicore.request.OperationUpdate;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.OperationService;

import javax.inject.Inject;

@PluginInfo(version = 1)
@InvokerInfo()
public class OperationsInvoker implements ListingInvoker<Operation, OperationFiltering> {

    @Inject
    private OperationService operationService;

    @Override
    @InvokerMethodInfo(displayName = "list all operations",description = "lists all operations")
    public PaginationResponse<Operation> listAll(OperationFiltering filter, SecurityContext securityContext) {
        operationService.validate(filter,securityContext);
        return operationService.getAllOperations(filter,securityContext);
    }

    @InvokerMethodInfo(displayName = "update Operation",description = "Updates Operation")
    public Operation update(OperationUpdate operationUpdate, SecurityContext securityContext) {
        operationService.validate(operationUpdate,securityContext);
        return operationService.updateOperation(operationUpdate,securityContext);
    }

    @Override
    public Class<OperationFiltering> getFilterClass() {
        return OperationFiltering.class;
    }

    @Override
    public Class<?> getHandlingClass() {
        return Operation.class;
    }
}
