package com.flexicore.service.impl;

import com.flexicore.data.UIComponentRepository;
import com.flexicore.data.jsoncontainers.UIComponentRegistrationContainer;
import com.flexicore.data.jsoncontainers.UIComponentsRegistrationContainer;
import com.flexicore.model.ui.UIComponent;
import com.flexicore.security.SecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by Asaf on 12/07/2017.
 */
@Primary
@Component
public class UIComponentService implements com.flexicore.service.UIComponentService {

    @Autowired
    private UIComponentRepository uiPluginRepository;


   private Logger logger = Logger.getLogger(getClass().getCanonicalName());

    @Autowired
    private FileResourceService fileResourceService;




    @Override
    public List<UIComponent> registerAndGetAllowedUIComponents(List<UIComponentRegistrationContainer> componentsToRegister, SecurityContext securityContext) {
        Map<String,UIComponentRegistrationContainer> externalIds=componentsToRegister.stream().collect(Collectors.toMap(f->f.getExternalId(),f->f,(a,b)->a));

        List<UIComponent> existing=uiPluginRepository.getExistingUIComponentsByIds(externalIds.keySet());


        List<UIComponent> accessiable=!existing.isEmpty()?uiPluginRepository.getAllowedUIComponentsByIds(existing.parallelStream().map(f->f.getId()).collect(Collectors.toSet()),securityContext):new ArrayList<>();

        List<UIComponentRegistrationContainer> componentsToCreate=externalIds.values().parallelStream().collect(Collectors.toList());
        List<UIComponent> toMerge=new ArrayList<>();

        for (UIComponentRegistrationContainer uiComponentRegistrationContainer : componentsToCreate) {
            UIComponent uiComponent= createUIComponentNoMerge(uiComponentRegistrationContainer,securityContext);
            toMerge.add(uiComponent);
        }

        uiPluginRepository.massMerge(toMerge);
        accessiable.addAll(toMerge);
        return accessiable;


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
