package com.flexicore.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.EventObject;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * will propagate events from one application contexts to all leaf application contexts ( only leafs are required as application context propagate the events to their parents by default)
 */
@Component
public class EventPropagator {

    @Autowired
    private FlexiCorePluginManager flexiCorePluginManager;
    private static final Set<Object> eventsInProcess=new ConcurrentSkipListSet<>(Comparator.comparing(System::identityHashCode));
    private Logger logger= LoggerFactory.getLogger(EventPropagator.class);

    /**
     * the eventsInProcess set is required to prevent infinite recursion
     * @param event
     */
    @EventListener
    public void handleContextStart(EventObject event) {
        if(!eventsInProcess.contains(event)){
            eventsInProcess.add(event);
            try {
                for (ApplicationContext applicationContext : flexiCorePluginManager.getPluginApplicationContexts()) {
                    if (event.getSource() != applicationContext) {
                        applicationContext.publishEvent(event);
                    }
                }
            }
            catch (Exception e)
            {
                logger.error("error while propagating event",e);
            }
            finally {
                eventsInProcess.remove(event);
            }
        }

    }
}
