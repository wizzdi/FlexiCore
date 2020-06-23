package com.flexicore.init;

import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class FlexiCoreBeanFactory extends DefaultListableBeanFactory {

    private final List<ApplicationContext> dependenciesContext=new ArrayList<>();

    public FlexiCoreBeanFactory() {
    }

    public FlexiCoreBeanFactory(BeanFactory parentBeanFactory) {
        super(parentBeanFactory);
    }

    public void addDependenciesContext(List<ApplicationContext> dependenciesContext){
        this.dependenciesContext.addAll(dependenciesContext);
    }


    @Override
    public Object resolveDependency(DependencyDescriptor descriptor, String requestingBeanName, Set<String> autowiredBeanNames, TypeConverter typeConverter) throws BeansException {
        try {
            return super.resolveDependency(descriptor, requestingBeanName, autowiredBeanNames, typeConverter);
        }
        catch (BeansException e){
            for (ApplicationContext applicationContext : dependenciesContext) {
                try {
                    return applicationContext.getAutowireCapableBeanFactory().resolveDependency(descriptor, requestingBeanName, autowiredBeanNames, typeConverter);
                }
                catch (BeansException ignored){

                }
            }
            throw e;
        }

    }

    @Override
    public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) throws BeansException {
        ObjectProvider<T> beanProvider = super.getBeanProvider(requiredType);

        return new ObjectProvider<T>() {
            @Override
            public T getObject() throws BeansException {
                try {
                    return beanProvider.getObject();
                } catch (NoSuchBeanDefinitionException e) {
                    for (ApplicationContext applicationContext : dependenciesContext) {
                        try {
                            return applicationContext.getAutowireCapableBeanFactory().getBeanProvider(requiredType).getObject();
                        } catch (NoSuchBeanDefinitionException e1) {
                        }
                    }
                    throw e;
                }
            }

            @Override
            public T getObject(Object... args) throws BeansException {
                try {
                    return beanProvider.getObject(args);
                } catch (NoSuchBeanDefinitionException e) {
                    for (ApplicationContext applicationContext : dependenciesContext) {
                        try {
                            return applicationContext.getAutowireCapableBeanFactory().getBeanProvider(requiredType).getObject(args);
                        } catch (NoSuchBeanDefinitionException e1) {
                        }
                    }
                    throw e;
                }
            }

            @Override
            @Nullable
            public T getIfAvailable() throws BeansException {

                T t = beanProvider.getIfAvailable();
                if (t == null) {
                    for (ApplicationContext applicationContext : dependenciesContext) {
                        t = applicationContext.getAutowireCapableBeanFactory().getBeanProvider(requiredType).getIfAvailable();
                        if (t != null) {
                            return t;
                        }

                    }
                }
                return t;
            }

            @Override
            @Nullable
            public T getIfUnique() throws BeansException {
                T t = beanProvider.getIfUnique();
                if (t == null) {
                    for (ApplicationContext applicationContext : dependenciesContext) {
                        t = applicationContext.getAutowireCapableBeanFactory().getBeanProvider(requiredType).getIfUnique();
                        if (t != null) {
                            return t;
                        }

                    }
                }
                return t;
            }

            @Override
            public Stream<T> stream() {
                Stream<T> stream = beanProvider.stream();
                for (ApplicationContext applicationContext : dependenciesContext) {
                    stream = Stream.concat(stream, applicationContext.getAutowireCapableBeanFactory().getBeanProvider(requiredType).stream());
                }
                return stream;

            }

            @Override
            public Stream<T> orderedStream() {
                Stream<T> stream = beanProvider.orderedStream();
                for (ApplicationContext applicationContext : dependenciesContext) {
                    stream = Stream.concat(stream, applicationContext.getAutowireCapableBeanFactory().getBeanProvider(requiredType).orderedStream());
                }
                return stream;
            }
        };
    }
}
