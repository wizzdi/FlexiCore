package com.flexicore.init;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.ResolvableType;

public class FlexiCoreApplicationContext extends AnnotationConfigApplicationContext {
    private final FlexiCoreBeanFactory flexiCoreBeanFactory;

    public FlexiCoreApplicationContext() {
       this(new FlexiCoreBeanFactory());
    }

    public FlexiCoreApplicationContext(FlexiCoreBeanFactory beanFactory) {
        super(beanFactory);
        this.flexiCoreBeanFactory=beanFactory;
    }


    @Override
    public FlexiCoreBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
        return flexiCoreBeanFactory;
    }

    @Override
    protected void publishEvent(Object event, ResolvableType eventType) {
        ApplicationEvent applicationEvent;
        if (event instanceof ApplicationEvent) {
            applicationEvent = (ApplicationEvent)event;
        } else {
            applicationEvent = new PayloadApplicationEvent(this, event);
            if (eventType == null) {
                eventType = ((PayloadApplicationEvent)applicationEvent).getResolvableType();
            }
        }
        super.publishEvent(applicationEvent,eventType);
    }
}
