package com.flexicore.service.impl;

import com.flexicore.data.impl.ConnectionSupportingRepository;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.model.Baseclass;
import com.flexicore.model.FilteringInformationHolder;
import com.flexicore.request.GetConnectedGeneric;
import com.flexicore.request.GetDisconnectedGeneric;
import com.flexicore.security.SecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Primary
@Component
public class ConnectionSupportingService implements com.flexicore.service.ConnectionSupportingService {

    @Autowired
    private ConnectionSupportingRepository connectionSupportingRepository;

    @Override
    public <Link extends Baseclass, Base extends Baseclass, BaseFilter extends FilteringInformationHolder, LinkFilter extends FilteringInformationHolder> List<Base> listConnected(GetConnectedGeneric<Base, Link, BaseFilter, LinkFilter> getConnectedGeneric, SecurityContext securityContext) {
        return connectionSupportingRepository.listConnected(getConnectedGeneric,securityContext);
    }

    @Override
    public <Link extends Baseclass, Base extends Baseclass, BaseFilter extends FilteringInformationHolder, LinkFilter extends FilteringInformationHolder> long countConnected(GetConnectedGeneric<Base, Link, BaseFilter, LinkFilter> getConnectedGeneric, SecurityContext securityContext) {
        return connectionSupportingRepository.countConnected(getConnectedGeneric,securityContext);
    }

    @Override
    public <Link extends Baseclass, Base extends Baseclass, BaseFilter extends FilteringInformationHolder, LinkFilter extends FilteringInformationHolder> List<Base> listDisconnected(GetDisconnectedGeneric<Base, Link, BaseFilter, LinkFilter> getDisconnectedGeneric, SecurityContext securityContext) {
        return connectionSupportingRepository.listDisconnected(getDisconnectedGeneric,securityContext);
    }

    @Override
    public <Link extends Baseclass, Base extends Baseclass, BaseFilter extends FilteringInformationHolder, LinkFilter extends FilteringInformationHolder> long countDisconnected(GetDisconnectedGeneric<Base, Link, BaseFilter, LinkFilter> getDisconnectedGeneric, SecurityContext securityContext) {
        return connectionSupportingRepository.countDisconnected(getDisconnectedGeneric,securityContext);
    }

    @Override
    public <Link extends Baseclass, Base extends Baseclass, BaseFilter extends FilteringInformationHolder, LinkFilter extends FilteringInformationHolder> PaginationResponse<Base> getConnected(GetConnectedGeneric<Base, Link, BaseFilter, LinkFilter> getConnectedGeneric, SecurityContext securityContext) {
        List<Base> list=listConnected(getConnectedGeneric,securityContext);
        long countConnected=countConnected(getConnectedGeneric,securityContext);
        return new PaginationResponse<>(list,getConnectedGeneric.getBaseFilter(),countConnected);
    }

    @Override
    public <Link extends Baseclass, Base extends Baseclass, BaseFilter extends FilteringInformationHolder, LinkFilter extends FilteringInformationHolder> PaginationResponse<Base> getDisconnected(GetDisconnectedGeneric<Base, Link, BaseFilter, LinkFilter> getDisconnectedGeneric, SecurityContext securityContext) {
        List<Base> list=listDisconnected(getDisconnectedGeneric,securityContext);
        long countDisconnected=countDisconnected(getDisconnectedGeneric,securityContext);
        return new PaginationResponse<>(list,getDisconnectedGeneric.getBaseFilter(),countDisconnected);
    }
}
