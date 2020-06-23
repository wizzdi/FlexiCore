package com.flexicore.init;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

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


}
