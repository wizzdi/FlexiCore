package com.flexicore.service.impl;

import com.flexicore.data.BaselinkRepository;
import com.flexicore.interfaces.Container;
import com.flexicore.model.Baseclass;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Created by Asaf on 13/11/2016.
 */
@Primary
@Component
public class ContainerService implements com.flexicore.service.ContainerService {


    @Autowired
    private BaselinkRepository baselinkRepository;
   private Logger logger = Logger.getLogger(getClass().getCanonicalName());

    @Autowired
    private PluginService pluginService;


    @Override
    public<T extends Baseclass> Container<T> getContainer(T contained){
        if(contained==null){
            return null;
        }
        Class<? extends Baseclass> c=contained.getClass();
        Collection<?> contianers= pluginService.getPlugins(Container.class,null,null);
        Container<T> selectedContainer=null;
        for (Object o : contianers) {
            Container<?> container= (Container<?>) o;
            if(contained.getClass().isAssignableFrom(container.getType())){
                selectedContainer= (Container<T>) container;
                break;
            }
        }
        if(selectedContainer==null){
            return null;
        }

        return selectedContainer.contain(contained);

    }
}
