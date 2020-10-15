package com.flexicore.service.impl;

import com.flexicore.annotations.Baseclassroot;
import com.flexicore.data.BaseclassRepository;
import com.flexicore.model.Baseclass;
import com.flexicore.model.FilteringInformationHolder;
import com.flexicore.model.QueryInformationHolder;
import com.flexicore.model.Tenant;
import com.flexicore.request.BaseclassCreate;
import com.flexicore.security.SecurityContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.BadRequestException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Primary
@Component
public class BaseclassNewService implements com.flexicore.service.BaseclassNewService {
    @Autowired
    @Baseclassroot
    private BaseclassRepository baseclassRepository;

    @Override
    public void populate(BaseclassCreate baseclassCreate, SecurityContext securityContext) {
        String tenantId = baseclassCreate.getTenantId();
        Tenant tenant = tenantId != null ? baseclassRepository.getByIdOrNull(tenantId, Tenant.class, null, securityContext) : null;
        baseclassCreate.setTenant(tenant);

    }

    @Override
    public void validate(BaseclassCreate baseclassCreate, SecurityContext securityContext) {
        populate(baseclassCreate, securityContext);
        if (baseclassCreate.getTenant() == null && baseclassCreate.getTenantId() != null) {
            throw new BadRequestException("No Tenant with id " + baseclassCreate.getTenantId());
        }

    }


    /**
     * keeping compatibility with code gen
     * @param filteringInformationHolder filteringinformationholder
     * @param securityContext security context
     */
    @Override
    public void validateFilter(FilteringInformationHolder filteringInformationHolder, SecurityContext securityContext){

    }

    /**
     * keeping compatibility with code gen
     *
     * @param baseclassCreate baseclass create
     * @param securityContext security context
     */
    @Override
    public void validateCreate(BaseclassCreate baseclassCreate, SecurityContext securityContext) {
        validate(baseclassCreate, securityContext);
    }


    @Override
    public boolean updateBaseclassNoMerge(BaseclassCreate baseclassCreate, Baseclass baseclass) {
        boolean update = false;
        if (baseclassCreate.getName() != null && !baseclassCreate.getName().equals(baseclass.getName())) {
            baseclass.setName(baseclassCreate.getName());
            update = true;
        }
        if (baseclassCreate.getSoftDelete() != null && !baseclassCreate.getSoftDelete().equals(baseclass.isSoftDelete())) {
            baseclass.setSoftDelete(baseclassCreate.getSoftDelete());
            update = true;
        }
        if (baseclassCreate.getDescription() != null && !baseclassCreate.getDescription().equals(baseclass.getDescription())) {
            baseclass.setDescription(baseclassCreate.getDescription());
            update = true;
        }
        if (baseclassCreate.getTenant() != null && (baseclass.getTenant() == null || !baseclassCreate.getTenant().getId().equals(baseclass.getTenant().getId()))) {
            baseclass.setTenant(baseclassCreate.getTenant());
            update = true;
        }
        if (baseclassCreate.supportingDynamic()&&baseclassCreate.any() != null && !baseclassCreate.any().isEmpty()) {
            Map<String, Object> jsonNode = baseclass.getJsonNode();
            if (jsonNode == null) {
                baseclass.setJsonNode(baseclassCreate.any());
                update = true;
            } else {
                for (Map.Entry<String, Object> entry : baseclassCreate.any().entrySet()) {
                    String key = entry.getKey();
                    Object newVal = entry.getValue();
                    Object val = jsonNode.get(key);
                    if (newVal!=null&&!newVal.equals(val)) {
                        jsonNode.put(key, newVal);
                        update = true;
                    }
                }
            }


        }
        return update;
    }

    @Override
    public <T extends Baseclass> T getByIdOrNull(String id, Class<T> c, List<String> batchString, SecurityContext securityContext) {
        return baseclassRepository.getByIdOrNull(id, c, batchString, securityContext);
    }

    @Override
    public <T extends Baseclass> List<T> listByIds(Class<T> aClass, Set<String> set, SecurityContext securityContext) {
        return baseclassRepository.listByIds(aClass, set, securityContext);
    }

    @Override
    @Transactional
    public void massMerge(List<?> toMerge) {
        baseclassRepository.massMerge(toMerge);
    }
}
