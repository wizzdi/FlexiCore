package com.flexicore.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.util.List;

@Component
public class IndexRepository {

    private static final Logger logger= LoggerFactory.getLogger(IndexRepository.class);
    @PersistenceContext
    private EntityManager em;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createIndex(Index index, String tableName) {

        try {
            Query existing = em.createNativeQuery("select indexname from pg_indexes where tablename=? and indexname=?");
            existing.setParameter(1,tableName);
            existing.setParameter(2,index.name());
            List<?> results=existing.getResultList();
            if(results.isEmpty()){
                Query query = em.createNativeQuery("create " + (index.unique() ? "unique " : "") + "index " + index.name() + " on " + tableName + "(" + index.columnList() + ")");
                query.executeUpdate();
                logger.info("created index " + (index.unique() ? "unique " : "") + index.name() + " on table " + tableName + "(" + index.columnList() + ")");
            }
            else{
                logger.debug("index " + (index.unique() ? "unique " : "") + index.name() + " on table " + tableName + "(" + index.columnList() + ") , already exists");
            }

        } catch (Exception e) {
            if (e.getMessage().toLowerCase().contains("already exists")) {
                logger.debug("index " + (index.unique() ? "unique " : "") + index.name() + " on table " + tableName + "(" + index.columnList() + ") , already exists");

            } else {
                logger.error("unable to create index", e);

            }
        }
    }
}
