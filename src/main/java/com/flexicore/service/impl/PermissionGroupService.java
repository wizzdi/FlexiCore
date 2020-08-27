/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.service.impl;

import com.flexicore.data.PermissionGroupRepository;
import com.flexicore.data.jsoncontainers.CreatePermissionGroupLinkRequest;
import com.flexicore.data.jsoncontainers.CreatePermissionGroupRequest;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.model.Baseclass;
import com.flexicore.model.PermissionGroup;
import com.flexicore.model.PermissionGroupToBaseclass;
import com.flexicore.request.PermissionGroupCopy;
import com.flexicore.request.PermissionGroupsFilter;
import com.flexicore.request.UpdatePermissionGroup;
import com.flexicore.security.SecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.BadRequestException;
import java.util.*;
import java.util.stream.Collectors;

@Primary
@Component
public class PermissionGroupService implements com.flexicore.service.PermissionGroupService {

    @Autowired
    private PermissionGroupRepository permissionGroupRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;


    @Override
    public <T extends Baseclass> T getByIdOrNull(String id, Class<T> c, List<String> batchString, SecurityContext securityContext) {
        return permissionGroupRepository.getByIdOrNull(id, c, batchString, securityContext);
    }

    @Override
    public <T extends Baseclass> List<T> listByIds(Class<T> c, Set<String> ids, SecurityContext securityContext) {
        return permissionGroupRepository.listByIds(c, ids, securityContext);
    }

    @Override
    public List<PermissionGroup> listPermissionGroups(PermissionGroupsFilter permissionGroupsFilter, SecurityContext securityContext) {
        return permissionGroupRepository.listPermissionGroups(permissionGroupsFilter, securityContext);
    }

    @Override
    public PaginationResponse<PermissionGroup> getAllPermissionGroups(PermissionGroupsFilter permissionGroupsFilter, SecurityContext securityContext) {
        List<PermissionGroup> permissionGroups = listPermissionGroups(permissionGroupsFilter, securityContext);
        long count = permissionGroupRepository.countPermissionGroups(permissionGroupsFilter, securityContext);
        return new PaginationResponse<>(permissionGroups, permissionGroupsFilter, count);
    }

    @Override
    public List<PermissionGroupToBaseclass> connectPermissionGroupsToBaseclasses(CreatePermissionGroupLinkRequest createPermissionGroupLinkRequest, SecurityContext securityContext) {
        List<Object> toMerge = new ArrayList<>();

        List<PermissionGroupToBaseclass> existingPermissionGroupsLinks = permissionGroupRepository.getExistingPermissionGroupsLinks(createPermissionGroupLinkRequest.getPermissionGroups(), createPermissionGroupLinkRequest.getBaseclasses());
        Map<String, Map<String, PermissionGroupToBaseclass>> existing = existingPermissionGroupsLinks
                .parallelStream().collect(Collectors.groupingBy(f -> f.getRightside().getId(), Collectors.toMap(f -> f.getLeftside().getId(), f -> f, (a, b) -> a)));
        for (Baseclass baseclass : createPermissionGroupLinkRequest.getBaseclasses()) {
            Map<String, PermissionGroupToBaseclass> entry = existing.computeIfAbsent(baseclass.getId(), f -> new HashMap<>());
            for (PermissionGroup permissionGroup : createPermissionGroupLinkRequest.getPermissionGroups()) {
                if (entry.get(permissionGroup.getId()) == null) {
                    PermissionGroupToBaseclass permissionGroupToBaseclass = new PermissionGroupToBaseclass("PermissionGroupLink", securityContext);
                    permissionGroupToBaseclass.setPermissionGroup(permissionGroup);
                    permissionGroupToBaseclass.setRightside(baseclass);
                    toMerge.add(permissionGroupToBaseclass);
                    entry.put(permissionGroup.getId(), permissionGroupToBaseclass);
                    existingPermissionGroupsLinks.add(permissionGroupToBaseclass);
                }
            }


        }
        permissionGroupRepository.massMerge(toMerge);

        return existingPermissionGroupsLinks;
    }

    @Override
    public List<PermissionGroupToBaseclass> getExistingPermissionGroupsLinks(List<PermissionGroup> permissionGroup, List<Baseclass> baseclasses) {
        return permissionGroupRepository.getExistingPermissionGroupsLinks(permissionGroup, baseclasses);
    }

    @Override
    public void validate(PermissionGroupsFilter permissionGroupsFilter, SecurityContext securityContext) {
        Set<String> baseclassIds = permissionGroupsFilter.getBaseclassIds();
        Map<String, Baseclass> permissionGroupMap = baseclassIds.isEmpty() ? new HashMap<>() : listByIds(Baseclass.class, baseclassIds, securityContext).parallelStream().collect(Collectors.toMap(f -> f.getId(), f -> f));
        baseclassIds.removeAll(permissionGroupMap.keySet());
        if (!baseclassIds.isEmpty()) {
            throw new BadRequestException("No Baseclasses with ids " + baseclassIds);
        }
        permissionGroupsFilter.setBaseclasses(new ArrayList<>(permissionGroupMap.values()));
    }

    @Override
    public void validate(PermissionGroupCopy permissionGroupCopy, SecurityContext securityContext) {
        String permissionGroupToCopyId = permissionGroupCopy.getPermissionGroupToCopyId();
        PermissionGroup permissionGroup = permissionGroupToCopyId != null ? getByIdOrNull(permissionGroupToCopyId, PermissionGroup.class, null, securityContext) : null;
        if (permissionGroup == null) {
            throw new BadRequestException("No Permission Group with id " + permissionGroupToCopyId);
        }
        permissionGroupCopy.setPermissionGroup(permissionGroup);
    }

    @Override
    public PermissionGroup createPermissionGroupNoMerge(CreatePermissionGroupRequest createPermissionGroupRequest, SecurityContext securityContext) {
        PermissionGroup permissionGroup = new PermissionGroup(createPermissionGroupRequest.getName(), securityContext);
        updatePermissionGroupNoMerge(permissionGroup, createPermissionGroupRequest);
        return permissionGroup;
    }

    @Override
    public boolean updatePermissionGroupNoMerge(PermissionGroup permissionGroup, CreatePermissionGroupRequest createPermissionGroupRequest) {
        boolean update = false;
        if (createPermissionGroupRequest.getName() != null && !createPermissionGroupRequest.getName().equals(permissionGroup.getName())) {
            permissionGroup.setName(createPermissionGroupRequest.getName());
            update = true;
        }
        if (createPermissionGroupRequest.getDescription() != null && !createPermissionGroupRequest.getDescription().equals(permissionGroup.getDescription())) {
            permissionGroup.setDescription(createPermissionGroupRequest.getDescription());
            update = true;
        }
        if (createPermissionGroupRequest.getExternalId() != null && !createPermissionGroupRequest.getExternalId().equals(permissionGroup.getExternalId())) {
            permissionGroup.setExternalId(createPermissionGroupRequest.getExternalId());
            update = true;
        }
        return update;
    }


    @Override
    public PermissionGroup createPermissionGroup(CreatePermissionGroupRequest createPermissionGroupRequest, SecurityContext securityContext) {

        PermissionGroup permissionGroup = createPermissionGroupNoMerge(createPermissionGroupRequest, securityContext);
        permissionGroupRepository.merge(permissionGroup);

        return permissionGroup;
    }

    @Override
    @Transactional
    public void merge(Object base) {
        permissionGroupRepository.merge(base);
    }

    @Override
    @Transactional
    public void massMerge(List<?> toMerge) {
        permissionGroupRepository.massMerge(toMerge);
    }

    @Override
    public PermissionGroup updatePermissionGroup(UpdatePermissionGroup updatePermissionGroup, SecurityContext securityContext) {
        PermissionGroup permissionGroup = updatePermissionGroup.getPermissionGroup();
        if (updatePermissionGroupNoMerge(permissionGroup, updatePermissionGroup)) {
            merge(permissionGroup);
        }
        return permissionGroup;
    }

    @Override
    public PermissionGroup copyPermissionGroup(PermissionGroupCopy permissionGroupCopy, SecurityContext securityContext) {
        PermissionGroup permissionGroupToCopy = permissionGroupCopy.getPermissionGroup();
        if (permissionGroupCopy.getName() == null) {
            permissionGroupCopy.setName(permissionGroupToCopy.getName());
        }
        if (permissionGroupCopy.getDescription() == null) {
            permissionGroupCopy.setDescription(permissionGroupToCopy.getDescription());
        }
        PermissionGroup permissionGroup = createPermissionGroup(permissionGroupCopy, securityContext);
        Map<String, Baseclass> links = permissionGroupRepository.getExistingPermissionGroupsLinks(Collections.singletonList(permissionGroupToCopy), null).parallelStream().map(f -> f.getRightside()).collect(Collectors.toMap(f -> f.getId(), f -> f, (a, b) -> a));
        connectPermissionGroupsToBaseclasses(new CreatePermissionGroupLinkRequest().setPermissionGroups(Collections.singletonList(permissionGroup)).setBaseclasses(new ArrayList<>(links.values())), securityContext);
        return permissionGroup;
    }
}
