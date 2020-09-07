package com.flexicore.data.impl;

import com.flexicore.annotations.InheritedComponent;
import com.flexicore.model.*;
import com.flexicore.request.GenericAddPredicates;
import com.flexicore.request.GenericJoinLinkToRoot;
import com.flexicore.request.GetConnectedGeneric;
import com.flexicore.request.GetDisconnectedGeneric;
import com.flexicore.security.SecurityContext;


import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;


@InheritedComponent
public class ConnectionSupportingRepository extends BaseclassRepository  {

    public <Link extends Baseclass, Base extends Baseclass, BaseFilter extends FilteringInformationHolder, LinkFilter extends FilteringInformationHolder> List<Base> listConnected(GetConnectedGeneric<Base, Link, BaseFilter, LinkFilter> getConnected, SecurityContext securityContext) {
        LinkFilter linkFilter = getConnected.getLinkFilter();
        BaseFilter baseFilter = getConnected.getBaseFilter();

        Class<Link> linkClass = getConnected.getLinkClass();
        Class<Base> wantedClass = getConnected.getWantedClass();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Base> q = cb.createQuery(wantedClass);
        Root<Base> r = q.from(wantedClass);
        Subquery<String> sub = createConnectionSubQuery(getConnected.getGenericJoinLinkToRoot(),getConnected.getLinkAddPredicates(), linkFilter, linkClass, cb, q, r);
        List<Predicate> preds = new ArrayList<>();
        getConnected.getBaseAddPredicates().addLinkPredicates(baseFilter,preds,r,cb);
        preds.add(r.get(Baseclass_.id).in(sub));
        QueryInformationHolder<Base> queryInformationHolder = new QueryInformationHolder<>(baseFilter, wantedClass, securityContext);
        return getAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    public <Link extends Baseclass, Base extends Baseclass, BaseFilter extends FilteringInformationHolder, LinkFilter extends FilteringInformationHolder> long countConnected(GetConnectedGeneric<Base, Link, BaseFilter, LinkFilter> getConnected, SecurityContext securityContext) {
        LinkFilter baselinkFilter = getConnected.getLinkFilter();
        BaseFilter baseFilter = getConnected.getBaseFilter();
        Class<Link> linkClass = getConnected.getLinkClass();
        Class<Base> wantedClass = getConnected.getWantedClass();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<Base> r = q.from(wantedClass);
        Subquery<String> sub = createConnectionSubQuery(getConnected.getGenericJoinLinkToRoot(),getConnected.getLinkAddPredicates(), baselinkFilter, linkClass, cb, q, r);
        List<Predicate> preds = new ArrayList<>();
        getConnected.getBaseAddPredicates().addLinkPredicates(baseFilter,preds,r,cb);
        preds.add(r.get(Baseclass_.id).in(sub));
        QueryInformationHolder<Base> queryInformationHolder = new QueryInformationHolder<>(baseFilter, wantedClass, securityContext);
        return countAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    public <Link extends Baseclass, Base extends Baseclass, BaseFilter extends FilteringInformationHolder, LinkFilter extends FilteringInformationHolder> List<Base> listDisconnected(GetDisconnectedGeneric<Base, Link, BaseFilter, LinkFilter> getDisconnected, SecurityContext securityContext) {
        LinkFilter linkFilter = getDisconnected.getLinkFilter();
        BaseFilter baseFilter = getDisconnected.getBaseFilter();
        Class<Base> wantedClass = getDisconnected.getWantedClass();
        Class<Link> linkClass = getDisconnected.getLinkClass();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Base> q = cb.createQuery(wantedClass);
        Root<Base> r = q.from(wantedClass);
        Subquery<String> sub = createConnectionSubQuery(getDisconnected.getGenericJoinLinkToRoot(),getDisconnected.getLinkAddPredicates(), linkFilter, linkClass, cb, q, r);
        List<Predicate> preds = new ArrayList<>();
        getDisconnected.getBaseAddPredicates().addLinkPredicates(baseFilter,preds,r,cb);

        preds.add(cb.not(r.get(Baseclass_.id).in(sub)));
        QueryInformationHolder<Base> queryInformationHolder = new QueryInformationHolder<>(baseFilter, wantedClass, securityContext);
        return getAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    public <Link extends Baseclass, Base extends Baseclass, BaseFilter extends FilteringInformationHolder, LinkFilter extends FilteringInformationHolder> long countDisconnected(GetDisconnectedGeneric<Base, Link, BaseFilter, LinkFilter> getDisconnected, SecurityContext securityContext) {
        LinkFilter linkFilter = getDisconnected.getLinkFilter();
        BaseFilter baseFilter = getDisconnected.getBaseFilter();
        Class<Base> wantedClass = getDisconnected.getWantedClass();
        Class<Link> linkClass = getDisconnected.getLinkClass();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<Base> r = q.from(wantedClass);
        Subquery<String> sub = createConnectionSubQuery(getDisconnected.getGenericJoinLinkToRoot(),getDisconnected.getLinkAddPredicates(), linkFilter, linkClass, cb, q, r);
        List<Predicate> preds = new ArrayList<>();
        getDisconnected.getBaseAddPredicates().addLinkPredicates(baseFilter,preds,r,cb);
        preds.add(cb.not(r.get(Baseclass_.id).in(sub)));
        QueryInformationHolder<Base> queryInformationHolder = new QueryInformationHolder<>(baseFilter, wantedClass, securityContext);
        return countAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    public <Link extends Baseclass, Base extends Baseclass, LinkFilter extends FilteringInformationHolder> Subquery<String> createConnectionSubQuery(
            GenericJoinLinkToRoot<Link, Base> genericJoinLinkToRoot, GenericAddPredicates<Link, LinkFilter> addLinkPredicates,
            LinkFilter filter, Class<Link> linkClass, CriteriaBuilder cb, CriteriaQuery<?> q, Root<Base> r) {
        Subquery<String> sub = q.subquery(String.class);
        Root<Link> linkRoot = sub.from(linkClass);
        Join<Link, Base> join = genericJoinLinkToRoot.joinLinkToRoot(linkRoot, cb);
        List<Predicate> subPreds = new ArrayList<>();
        addLinkPredicates.addLinkPredicates(filter, subPreds, linkRoot, cb);
        subPreds.add(cb.isFalse(linkRoot.get(Baseclass_.softDelete)));
        Predicate[] subPredsArr = new Predicate[subPreds.size()];
        subPreds.toArray(subPredsArr);
        sub.select(join.get(Baseclass_.id)).where(subPredsArr);
        return sub;
    }


}
