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

import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
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

        if (pair != null && pair.getLeft().equals(InheritanceType.SINGLE_TABLE)) {
            Class<?> parent = pair.getRight();
            Table inherited = parent.getAnnotation(Table.class);
            tableName = inherited == null || inherited.name().isEmpty() ? parent.getSimpleName() : inherited.name();
        } else {
            tableName = table == null || table.name().isEmpty() ? claz.getSimpleName() : table.name();
        }
        if (table != null) {
            for (Index index : table.indexes()) {


                indexRepository.createIndex(index, tableName);
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
