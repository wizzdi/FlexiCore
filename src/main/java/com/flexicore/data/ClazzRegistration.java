package com.flexicore.data;

import com.flexicore.annotations.InheritedComponent;
import com.flexicore.model.Clazz;
import org.springframework.transaction.annotation.Transactional;


import javax.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@InheritedComponent
public class ClazzRegistration {


    @PersistenceContext
    private EntityManager em;


    private static final Logger logger = LoggerFactory.getLogger(ClazzRegistration.class);

    public void register(Clazz clazz) {
        em.persist(clazz);
    }


}