package com.flexicore.data;

import com.flexicore.annotations.InheritedComponent;
import com.flexicore.data.impl.BaseclassRepository;
import com.flexicore.model.FilteringInformationHolder;
import com.flexicore.model.QueryInformationHolder;
import com.flexicore.model.ui.*;
import com.flexicore.model.ui.plugins.*;
import com.flexicore.security.SecurityContext;

//import javax.ejb.Stateless;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Asaf on 12/07/2017.
 */
@InheritedComponent
public class UIPluginRepository extends BaseclassRepository {

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



    public List<UIPlugin> listUIPluginsByInterface(UIInterface uiInterface, FilteringInformationHolder filteringInformationHolder, int pageSize, int currentPage, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UIPlugin> q = cb.createQuery(UIPlugin.class);
        Root<UIPlugin> r = q.from(UIPlugin.class);
        Join<UIPlugin,UIPluginToUIInterface> join=r.join(UIPlugin_.uiPluginToUIInterfaces);
        Predicate p = cb.equal(join.get(UIPluginToUIInterface_.uiInterface),uiInterface);
        List<Predicate> preds= new ArrayList<>();
        preds.add(p);
        QueryInformationHolder<UIPlugin> queryInformationHolder= new QueryInformationHolder<>(filteringInformationHolder,pageSize,currentPage,UIPlugin.class,securityContext);
        return getAllFiltered(queryInformationHolder,preds,cb,q,r);
    }
}
