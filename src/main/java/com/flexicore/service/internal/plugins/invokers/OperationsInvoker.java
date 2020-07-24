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
import org.pf4j.Extension;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

@PluginInfo(version = 1)
@InvokerInfo()
@Extension
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
        Operation operation=operationUpdate.getId()!=null?operationService.getByIdOrNull(operationUpdate.getId(),Operation.class,null,securityContext):null;
        if(operation==null){
            throw new BadRequestException("No Operation with id "+operationUpdate.getId());
        }
        operationUpdate.setOperation(operation);
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
