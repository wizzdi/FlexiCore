package com.flexicore.service.impl;

import com.flexicore.data.UIComponentRepository;
import com.flexicore.data.jsoncontainers.CreatePermissionGroupLinkRequest;
import com.flexicore.data.jsoncontainers.CreatePermissionGroupRequest;
import com.flexicore.data.jsoncontainers.UIComponentRegistrationContainer;
import com.flexicore.data.jsoncontainers.UIComponentsRegistrationContainer;
import com.flexicore.model.Baseclass;
import com.flexicore.model.PermissionGroup;
import com.flexicore.model.ui.UIComponent;
import com.flexicore.request.PermissionGroupsFilter;
import com.flexicore.security.SecurityContext;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.ws.rs.BadRequestException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Asaf on 12/07/2017.
 */
@Primary
@Component
public class UIComponentService implements com.flexicore.service.UIComponentService {

    @Autowired
    private UIComponentRepository uiPluginRepository;

    @Autowired
    private SecurityService securityService;
    @Autowired
    private PermissionGroupService permissionGroupService;

   private Logger logger = Logger.getLogger(getClass().getCanonicalName());

    @Autowired
    private FileResourceService fileResourceService;




    @Override
    public List<UIComponent> registerAndGetAllowedUIComponents(List<UIComponentRegistrationContainer> componentsToRegister, SecurityContext securityContext) {
        SecurityContext adminSecutiryContext=securityService.getAdminUserSecurityContext();
        Map<String,UIComponentRegistrationContainer> externalIds=componentsToRegister.stream().collect(Collectors.toMap(f->f.getExternalId(),f->f,(a,b)->a));
        List<UIComponent> existing=new ArrayList<>();
        for (List<String> externalIdsBatch : Lists.partition(new ArrayList<>(externalIds.keySet()), 50)) {
            existing.addAll(uiPluginRepository.getExistingUIComponentsByIds(new HashSet<>(externalIdsBatch)));
        }
        Set<String> groupExternalIds=componentsToRegister.stream().filter(f->f.getGroups()!=null).map(f->f.getGroups().split(",")).flatMap(Stream::of).collect(Collectors.toSet());
        Map<String, PermissionGroup> permissionGroupMap=groupExternalIds.isEmpty()?new HashMap<>():permissionGroupService.listPermissionGroups(new PermissionGroupsFilter().setExternalIds(groupExternalIds),null).stream().collect(Collectors.toMap(f->f.getExternalId(),f->f,(a,b)->a));
        Map<String,UIComponent> existingMap=existing.stream().collect(Collectors.toMap(f->f.getExternalId(),f->f,(a,b)->a));

        List<UIComponent> accessible=new ArrayList<>();
        for (List<String> idsBatch : Lists.partition(existing.stream().map(f->f.getId()).collect(Collectors.toList()),50)) {
            accessible.addAll(uiPluginRepository.listByIds(UIComponent.class,new HashSet<>(idsBatch),securityContext));
        }

        List<UIComponentRegistrationContainer> componentsToCreate=externalIds.values().parallelStream().collect(Collectors.toList());
        List<Object> toMerge=new ArrayList<>();
        boolean isAdmin=securityContext.getUser().getId().equals(adminSecutiryContext.getUser().getId());
        Map<String,List<Baseclass>> permissionGroupExternalIdToUIComponent=new HashMap<>();
        for (UIComponentRegistrationContainer uiComponentRegistrationContainer : componentsToCreate) {
            UIComponent uiComponent= existingMap.get(uiComponentRegistrationContainer.getExternalId());

            if(uiComponent==null){
                uiComponent= createUIComponentNoMerge(uiComponentRegistrationContainer,adminSecutiryContext);
                toMerge.add(uiComponent);
                existingMap.put(uiComponent.getExternalId(),uiComponent);
                if(isAdmin){
                    accessible.add(uiComponent);
                }
            }
            else{
                if(uiComponent.isSoftDelete()){
                    uiComponent.setSoftDelete(false);
                    toMerge.add(uiComponent);
                }
            }
            Set<String> permissionGroupsExternalIds=uiComponentRegistrationContainer.getGroups()==null?new HashSet<>():Stream.of(uiComponentRegistrationContainer.getGroups().split(",")).collect(Collectors.toSet());
            for (String permissionGroupsExternalId : permissionGroupsExternalIds) {
                CreatePermissionGroupRequest createPermissionGroupRequest=new CreatePermissionGroupRequest()
                        .setExternalId(permissionGroupsExternalId)
                        .setName(permissionGroupsExternalId)
                        .setDescription(permissionGroupsExternalId);
                PermissionGroup permissionGroup=permissionGroupMap.get(permissionGroupsExternalId);
                if(permissionGroup==null){
                    permissionGroup=permissionGroupService.createPermissionGroupNoMerge(createPermissionGroupRequest,adminSecutiryContext);
                    permissionGroupMap.put(permissionGroupsExternalId,permissionGroup);
                    toMerge.add(permissionGroup);
                }
                else{
                    if(permissionGroupService.updatePermissionGroupNoMerge(permissionGroup,createPermissionGroupRequest)){
                        toMerge.add(permissionGroup);
                    }
                }
                permissionGroupExternalIdToUIComponent.computeIfAbsent(permissionGroupsExternalId,f->new ArrayList<>()).add(uiComponent);
            }



        }


        uiPluginRepository.massMerge(toMerge);
        for (Map.Entry<String, List<Baseclass>> stringListEntry : permissionGroupExternalIdToUIComponent.entrySet()) {
            PermissionGroup permissionGroup=permissionGroupMap.get(stringListEntry.getKey());
            List<Baseclass> baseclasses=stringListEntry.getValue();
            permissionGroupService.connectPermissionGroupsToBaseclasses(new CreatePermissionGroupLinkRequest().setPermissionGroups(Collections.singletonList(permissionGroup)).setBaseclasses(baseclasses),adminSecutiryContext);

        }
        return accessible;


    }

    private UIComponent createUIComponentNoMerge(UIComponentRegistrationContainer uiComponentRegistrationContainer, SecurityContext securityContext) {
        UIComponent uiComponent=new UIComponent(uiComponentRegistrationContainer.getName(),securityContext);
        uiComponent.setExternalId(uiComponentRegistrationContainer.getExternalId());
        uiComponent.setDescription(uiComponentRegistrationContainer.getDescription());
        return uiComponent;

    }



    @Override
    public void validate(UIComponentsRegistrationContainer uiComponentsRegistrationContainer) {

        for (UIComponentRegistrationContainer uiComponentRegistrationContainer : uiComponentsRegistrationContainer.getComponentsToRegister()) {
            if(uiComponentRegistrationContainer.getExternalId()==null){
                throw new BadRequestException("uiComponentRegistrationContainer.externalId is mandatory");
            }
        }
    }
}
