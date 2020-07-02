package com.flexicore.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;

@Component
public class IndexRepository {

    private static final Logger logger= LoggerFactory.getLogger(IndexRepository.class);
    @PersistenceContext
    private EntityManager em;

    @Transactional(noRollbackFor = PersistenceException.class)
    public void createIndex(Index index, String tableName) {
        try {
            Query query = em.createNativeQuery("create " + (index.unique() ? "unique " : "") + "index " + index.name() + " on " + tableName + "(" + index.columnList() + ")");
            query.executeUpdate();
            logger.info("created index " + (index.unique() ? "unique " : "") + index.name() + " on table " + tableName + "(" + index.columnList() + ")");
        } catch (Exception e) {
            if (e.getMessage().toLowerCase().contains("already exists")) {
                logger.debug("index " + (index.unique() ? "unique " : "") + index.name() + " on table " + tableName + "(" + index.columnList() + ") , already exists");

            } else {
                logger.error("unable to create index", e);

            }
        }
    }
}
