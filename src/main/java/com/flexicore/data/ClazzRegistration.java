package com.flexicore.data;

import com.flexicore.annotations.InheritedComponent;
import com.flexicore.model.Clazz;
import org.springframework.transaction.annotation.Transactional;

import javax.ejb.Stateless;
import javax.persistence.*;
import java.util.logging.Level;
import java.util.logging.Logger;


@InheritedComponent
public class ClazzRegistration {


    @PersistenceContext
    private EntityManager em;


    private Logger logger = Logger.getLogger(getClass().getCanonicalName());

    public void register(Clazz clazz) {
        em.persist(clazz);
    }

    @Transactional(noRollbackFor = PersistenceException.class)
    public void createIndex(Index index, String tableName) {
        try {
            Query query = em.createNativeQuery("create " + (index.unique() ? "unique " : "") + "index " + index.name() + " on " + tableName + "(" + index.columnList() + ")");
            query.executeUpdate();
            logger.info("created index " + (index.unique() ? "unique " : "") + index.name() + " on table " + tableName + "(" + index.columnList() + ")");
        } catch (Exception e) {
            if (e.getMessage().toLowerCase().contains("already exists")) {
                logger.fine("index " + (index.unique() ? "unique " : "") + index.name() + " on table " + tableName + "(" + index.columnList() + ") , already exists");

            } else {
                logger.log(Level.SEVERE, "unable to create index", e);

            }
        }
    }
}