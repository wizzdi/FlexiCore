package com.flexicore.init;

import com.flexicore.events.PluginsLoadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EventObject;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * will propagate events from one application contexts to all leaf application contexts ( only leafs are required as application context propagate the events to their parents by default)
 */
@Component
public class EventPropagator {

    @Autowired
    private FlexiCorePluginManager flexiCorePluginManager;
    private static final Set<Object> eventsInProcess = new ConcurrentSkipListSet<>(Comparator.comparing(System::identityHashCode));
    private Logger logger = LoggerFactory.getLogger(EventPropagator.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    private static final AtomicBoolean init = new AtomicBoolean(false);

    @EventListener
    public void onContextRefreshed(ContextRefreshedEvent contextRefreshedEvent) {
        if (contextRefreshedEvent.getApplicationContext().getId() != null && contextRefreshedEvent.getApplicationContext().getId().equals(applicationContext.getId())) {
            if (init.compareAndSet(false, true)) {
                logger.info("sending Plugins Loaded Event");
                applicationEventPublisher.publishEvent(new PluginsLoadedEvent(applicationContext, new ArrayList<>()));
            }
        }

    }

    /**
     * the eventsInProcess set is required to prevent infinite recursion
     *
     * @param event event to send to all plugin contexts
     */
    @EventListener
    public void handleContextStart(EventObject event) {
        if (!eventsInProcess.contains(event)) {
            Object eventToPrint = event instanceof PayloadApplicationEvent ? ((PayloadApplicationEvent<?>) event).getPayload() : event;

            logger.debug("Propagating event " + eventToPrint);
            eventsInProcess.add(event);
            try {
                for (ApplicationContext applicationContext : flexiCorePluginManager.getPluginApplicationContexts()) {
                    try {

                        if (event.getSource() != applicationContext) {
                            Object contextId = applicationContext.getClassLoader() instanceof FlexiCorePluginClassLoader ? applicationContext.getClassLoader() : applicationContext.getId();
                            logger.debug("Propagating event " + eventToPrint + " to context " + contextId);
                            applicationContext.publishEvent(event);
                        }
                    } catch (Exception e) {
                        logger.error("error while propagating event: " + eventToPrint, e);
                    }
                }
            } finally {
                eventsInProcess.remove(event);
            }
        }

    }
}
