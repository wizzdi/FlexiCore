package com.flexicore.init;

import com.flexicore.data.IndexRepository;
import com.flexicore.events.PluginsLoadedEvent;
import com.flexicore.model.Baseclass;
import com.flexicore.provider.EntitiesHolder;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class IndexCreator {

    private static final AtomicBoolean init=new AtomicBoolean(false);
    private static final Logger logger= LoggerFactory.getLogger(IndexCreator.class);

    @Autowired
    private IndexRepository indexRepository;

    @Autowired
    private EntitiesHolder entitiesHolder;

    @Value("${flexicore.inheritence.strategy:SINGLE_TABLE}")
    private String inheritanceType;
    @Async
    @EventListener
    public void init(PluginsLoadedEvent o){
        if(init.compareAndSet(false,true)){
            for (Class<?> entity : entitiesHolder.getEntities()) {
                try {
                    createIndex(entity);
                }
                catch (Exception e){
                    logger.error("Failed creating index for "+entity.getName(),e);
                }
            }
        }

    }

    private void createIndex(Class<?> claz) {
        Table table = claz.getAnnotation(Table.class);
        String tableName;
        Pair<InheritanceType, Class<?>> pair = getInheritedTableName(claz);
        boolean singleTable=false;
        if (pair != null && (singleTable=pair.getLeft().equals(InheritanceType.SINGLE_TABLE))) {
            Class<?> parent = pair.getRight();
            Table inheritedTable = parent.getAnnotation(Table.class);
            tableName = inheritedTable == null || inheritedTable.name().isEmpty() ? parent.getSimpleName() : inheritedTable.name();
        } else {
            tableName = table == null || table.name().isEmpty() ? claz.getSimpleName() : table.name();
        }
        if (table != null) {
            for (Index index : table.indexes()) {

                try {
                    indexRepository.createIndex(index, tableName,singleTable);
                }
                catch (RuntimeException rollbackException){
                    logger.debug("failed creating index",rollbackException);
                }
            }
        }
    }

    private Pair<InheritanceType, Class<?>> getInheritedTableName(Class<?> orginal) {
        for (Class<?> current = orginal; current.getSuperclass() != null; current = current.getSuperclass()) {
            Inheritance inheritance = current.getDeclaredAnnotation(Inheritance.class);
            if (inheritance != null) {
                InheritanceType strategy =  Baseclass.class.equals(current)?InheritanceType.valueOf(inheritanceType):inheritance.strategy();
                return Pair.of(strategy, current);
            }
        }
        return null;
    }
}
