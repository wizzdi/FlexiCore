package com.flexicore.data;

import com.flexicore.annotations.InheritedComponent;
import com.flexicore.data.impl.BaseclassRepository;
import com.flexicore.model.QueryInformationHolder;
import com.flexicore.model.ui.UIComponent;
import com.flexicore.model.ui.UIComponent_;
import com.flexicore.security.SecurityContext;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//import javax.ejb.Stateless;

/**
 * Created by Asaf on 12/07/2017.
 */
@InheritedComponent
public class UIComponentRepository extends BaseclassRepository {

    public List<UIComponent> getExistingUIComponentsByIds(Set<String> ids) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UIComponent> q = cb.createQuery(UIComponent.class);
        Root<UIComponent> r = q.from(UIComponent.class);
        Predicate p = r.get(UIComponent_.externalId).in(ids);
        q.select(r).where(p);
        TypedQuery<UIComponent> query=em.createQuery(q);
        return query.getResultList();
    }

    public List<UIComponent> getAllowedUIComponentsByIds(Set<String> ids, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UIComponent> q = cb.createQuery(UIComponent.class);
        Root<UIComponent> r = q.from(UIComponent.class);
        Predicate p = r.get(UIComponent_.id).in(ids);
        List<Predicate> preds= new ArrayList<>();
        preds.add(p);
        QueryInformationHolder<UIComponent> queryInformationHolder= new QueryInformationHolder<>(UIComponent.class,securityContext);
        return getAllFiltered(queryInformationHolder,preds,cb,q,r);

    }



}
