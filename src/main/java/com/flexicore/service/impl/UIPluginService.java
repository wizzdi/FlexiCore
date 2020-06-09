package com.flexicore.service.impl;

import com.flexicore.data.UIPluginRepository;
import com.flexicore.data.jsoncontainers.UIComponentsRegistrationContainer;
import com.flexicore.model.FilteringInformationHolder;
import com.flexicore.data.jsoncontainers.UIComponentRegistrationContainer;
import com.flexicore.data.jsoncontainers.UIContainerRegistrationContainer;
import com.flexicore.data.jsoncontainers.UIPluginCreationContainer;
import com.flexicore.model.Baseclass;
import com.flexicore.model.FileResource;
import com.flexicore.model.QueryInformationHolder;
import com.flexicore.model.ui.*;
import com.flexicore.model.ui.plugins.UIInterface;
import com.flexicore.model.ui.plugins.UIPlugin;
import com.flexicore.model.ui.plugins.UIPluginToFileResource;
import com.flexicore.model.ui.plugins.UIPluginToUIInterface;
import com.flexicore.security.SecurityContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Autowired;
import javax.ws.rs.BadRequestException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by Asaf on 12/07/2017.
 */
@Primary
@Component
public class UIPluginService  implements com.flexicore.service.UIPluginService {

    @Autowired
    private UIPluginRepository uiPluginRepository;


   private Logger logger = Logger.getLogger(getClass().getCanonicalName());

    @Autowired
    private FileResourceService fileResourceService;


    @Override
    public List<UIInterface> listUIInterfaces(FilteringInformationHolder filteringInformationHolder, int pageSize, int currentPage, SecurityContext securityContext) {
        QueryInformationHolder<UIInterface> queryInformationHolder= new QueryInformationHolder<>(filteringInformationHolder,pageSize,currentPage,UIInterface.class,securityContext);
        return uiPluginRepository.getAllFiltered(queryInformationHolder);
    }

    @Override
    public List<UIPlugin> listUIPluginsByInterface(UIInterface uiInterface, FilteringInformationHolder filteringInformationHolder, int pageSize, int currentPage, SecurityContext securityContext) {
        return uiPluginRepository.listUIPluginsByInterface(uiInterface,filteringInformationHolder,pageSize,currentPage,securityContext);
    }

    @Override
    public UIPlugin registerUIPlugin(UIPluginCreationContainer uiPluginCreationContainer, SecurityContext securityContext) {
        List<Object> toMerge= new ArrayList<>();
        UIPlugin uiPlugin=createUIPluginNoMerge(uiPluginCreationContainer,securityContext);
        toMerge.add(uiPlugin);
        for (UIInterface uiInterface : uiPluginCreationContainer.getUiInterfaces()) {
            UIPluginToUIInterface link=linkUIPluginToUIInterfaceNoMerge(uiPlugin,uiInterface,securityContext);
            toMerge.add(link);
        }
        for (FileResource fileResource : uiPluginCreationContainer.getFileResources()) {
            UIPluginToFileResource uiPluginToFileResource=createUIToFileResourceNoMerge(uiPlugin,fileResource,securityContext);
            toMerge.add(uiPluginToFileResource);
        }
        uiPluginRepository.massMerge(toMerge);
        return uiPlugin;
    }

    private UIPluginToFileResource createUIToFileResourceNoMerge(UIPlugin uiPlugin, FileResource fileResource, SecurityContext securityContext) {
        UIPluginToFileResource uiPluginToFileResource=new UIPluginToFileResource("link",securityContext);
        uiPluginToFileResource.setUiPlugin(uiPlugin);
        uiPluginToFileResource.setFileResource(fileResource);
        uiPlugin.getUiPluginToFileResources().add(uiPluginToFileResource);

        return uiPluginToFileResource;
    }

    private UIPluginToUIInterface linkUIPluginToUIInterfaceNoMerge(UIPlugin uiPlugin, UIInterface uiInterface, SecurityContext securityContext) {
        UIPluginToUIInterface link=new UIPluginToUIInterface("link",securityContext);
        link.setUiPlugin(uiPlugin);
        link.setUiInterface(uiInterface);
        uiPlugin.getUiPluginToUIInterfaces().add(link);
        uiInterface.getUiPluginToUIInterfaces().add(link);
        return link;
    }

    private UIPlugin createUIPluginNoMerge(UIPluginCreationContainer uiPluginCreationContainer, SecurityContext securityContext) {
        UIPlugin uiPlugin=new UIPlugin(uiPluginCreationContainer.getName(),securityContext);
        uiPlugin.setDescription(uiPluginCreationContainer.getDescription());



        return uiPlugin;
    }

    @Override
    public UIInterface createUIInterface(String name, SecurityContext securityContext) {
        UIInterface uiInterface=new UIInterface(name,securityContext);
        uiInterface.setId(getUIInterfaceId(name));
        uiPluginRepository.merge(uiInterface);
        return uiInterface;

    }

    @Override
    public String getUIInterfaceId(String name){
        return Baseclass.generateUUIDFromString(UIInterface.class.getSimpleName()+"."+name);
    }

    @Override
    public List<UIComponent> registerAndGetAllowedUIComponents(List<UIComponentRegistrationContainer> componentsToRegister, SecurityContext securityContext) {
        Map<String,UIComponentRegistrationContainer> externalIds=new HashMap<>();
        for (UIComponentRegistrationContainer uiComponentRegistrationContainer : componentsToRegister) {
            externalIds.putAll(getUIComponentsMapping(uiComponentRegistrationContainer));
        }
        List<UIComponent> existing=uiPluginRepository.getExistingUIComponentsByIds(externalIds.keySet());
        Map<String,UIContainer> componentToContainerMap=new HashMap<>();


        for (UIComponent uiComponent : existing) {
            UIComponentRegistrationContainer uiComponentRegistrationContainer=externalIds.remove(uiComponent.getExternalId());
            if(uiComponentRegistrationContainer instanceof UIContainerRegistrationContainer){
                for (UIComponentRegistrationContainer componentRegistrationContainer : ((UIContainerRegistrationContainer) uiComponentRegistrationContainer).getComponents()) {
                    componentToContainerMap.put(componentRegistrationContainer.getExternalId(), (UIContainer) uiComponent);

                }

            }
        }

        List<UIComponent> accessiable=!existing.isEmpty()?uiPluginRepository.getAllowedUIComponentsByIds(existing.parallelStream().map(f->f.getId()).collect(Collectors.toSet()),securityContext):new ArrayList<>();

       List<UIContainerRegistrationContainer> containersToCreate=externalIds.values().parallelStream().filter(f->(f instanceof UIContainerRegistrationContainer)).map(f->(UIContainerRegistrationContainer)f).collect(Collectors.toList());
        List<UIComponentRegistrationContainer> componentsToCreate=externalIds.values().parallelStream().filter(f->!(f instanceof UIContainerRegistrationContainer)).collect(Collectors.toList());
        List<UIComponent> toMerge=new ArrayList<>();

        for (UIContainerRegistrationContainer uiContainerRegistrationContainer : containersToCreate) {
            UIContainer uiContainer= createUIContainerNoMerge(uiContainerRegistrationContainer,securityContext);
            for (UIComponentRegistrationContainer uiComponentRegistrationContainer : uiContainerRegistrationContainer.getComponents()) {
                componentToContainerMap.put(uiComponentRegistrationContainer.getExternalId(),uiContainer);

            }
            toMerge.add(uiContainer);

        }
        for (UIComponentRegistrationContainer uiComponentRegistrationContainer : componentsToCreate) {
            UIComponent uiComponent= createUIComponentNoMerge(uiComponentRegistrationContainer,componentToContainerMap.get(uiComponentRegistrationContainer.getExternalId()),securityContext);
            toMerge.add(uiComponent);
        }

        uiPluginRepository.massMerge(toMerge);
        accessiable.addAll(toMerge);
        return accessiable;


    }

    private UIComponent createUIComponentNoMerge(UIComponentRegistrationContainer uiComponentRegistrationContainer, UIContainer uiContainer, SecurityContext securityContext) {
        UIComponent uiComponent=new UIComponent(uiComponentRegistrationContainer.getName(),securityContext);
        uiComponent.setExternalId(uiComponentRegistrationContainer.getExternalId());
        uiComponent.setUiContainer(uiContainer);
        uiComponent.setDescription(uiComponentRegistrationContainer.getDescription());
        //uiContainer.getUiComponents().add(uiComponent);
        return uiComponent;

    }

    private UIContainer createUIContainerNoMerge(UIContainerRegistrationContainer uiContainerRegistrationContainer, SecurityContext securityContext) {
        UIContainer uiContainer=new UIContainer(uiContainerRegistrationContainer.getName(),securityContext);
        uiContainer.setExternalId(uiContainerRegistrationContainer.getExternalId());
        uiContainer.setDescription(uiContainerRegistrationContainer.getDescription());
        return uiContainer;
    }

    private Map<String,UIComponentRegistrationContainer> getUIComponentsMapping(UIComponentRegistrationContainer uiComponentRegistrationContainer){
        Map<String,UIComponentRegistrationContainer> map= new HashMap<>();
        map.put(uiComponentRegistrationContainer.getExternalId(),uiComponentRegistrationContainer);
        if(uiComponentRegistrationContainer instanceof UIContainerRegistrationContainer){
            for (UIComponentRegistrationContainer componentRegistrationContainer : ((UIContainerRegistrationContainer) uiComponentRegistrationContainer).getComponents()) {
                map.putAll(getUIComponentsMapping(componentRegistrationContainer));
            }
        }
        return map;
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
