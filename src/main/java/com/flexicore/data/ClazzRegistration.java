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


}