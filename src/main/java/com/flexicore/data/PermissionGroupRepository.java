/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.data;

import com.flexicore.annotations.InheritedComponent;
import com.flexicore.data.impl.BaseclassRepository;
import com.flexicore.model.*;
import com.flexicore.request.PermissionGroupsFilter;
import com.flexicore.security.SecurityContext;


import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@InheritedComponent
public class PermissionGroupRepository extends BaseclassRepository {

    public PermissionGroupRepository() {
        // TODO Auto-generated constructor stub
    }

    public List<PermissionGroupToBaseclass> getExistingPermissionGroupsLinks(List<PermissionGroup> permissionGroup, List<Baseclass> baseclasses) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<PermissionGroupToBaseclass> q = cb.createQuery(PermissionGroupToBaseclass.class);
        Root<PermissionGroupToBaseclass> r = q.from(PermissionGroupToBaseclass.class);

        Predicate pred = cb.and(
                cb.not(cb.isTrue(r.get(PermissionGroupToBaseclass_.softDelete)))
        );
        if(permissionGroup!=null && !permissionGroup.isEmpty()){
            pred=cb.and(pred,r.get(PermissionGroupToBaseclass_.leftside).in(permissionGroup));
        }
        if(baseclasses!=null && !baseclasses.isEmpty()){
            pred=cb.and(pred,r.get(PermissionGroupToBaseclass_.rightside).in(baseclasses));
        }
        q.select(r).where(pred);
        TypedQuery<PermissionGroupToBaseclass> query = em.createQuery(q);
        return query.getResultList();
    }

    public long countPermissionGroups(PermissionGroupsFilter permissionGroupsFilter, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<PermissionGroup> r = q.from(PermissionGroup.class);
        List<Predicate> preds=new ArrayList<>();
        addPermissionGroupsPredicate(preds,r,cb,permissionGroupsFilter);
        QueryInformationHolder<PermissionGroup> queryInformationHolder=new QueryInformationHolder<>(permissionGroupsFilter,PermissionGroup.class,securityContext);
        return countAllFiltered(queryInformationHolder,preds,cb,q,r);

    }

    private void addPermissionGroupsPredicate(List<Predicate> preds, Root<PermissionGroup> r, CriteriaBuilder cb, PermissionGroupsFilter permissionGroupsFilter) {
        if(permissionGroupsFilter.getBaseclasses()!=null && !permissionGroupsFilter.getBaseclasses().isEmpty()){
            Set<String> ids=permissionGroupsFilter.getBaseclasses().parallelStream().map(f->f.getId()).collect(Collectors.toSet());
            Join<PermissionGroup,PermissionGroupToBaseclass> linkJoin=r.join(PermissionGroup_.links);
            Join<PermissionGroupToBaseclass,Baseclass> baseclassJoin=linkJoin.join(PermissionGroupToBaseclass_.rightside);
            preds.add(baseclassJoin.get(Baseclass_.id).in(ids));
        }
        if(permissionGroupsFilter.getExternalIds()!=null && !permissionGroupsFilter.getExternalIds().isEmpty()){
            Set<String> ids=permissionGroupsFilter.getExternalIds();
            preds.add(r.get(PermissionGroup_.externalId).in(ids));
        }
    }

    public List<PermissionGroup> listPermissionGroups(PermissionGroupsFilter permissionGroupsFilter, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<PermissionGroup> q = cb.createQuery(PermissionGroup.class);
        Root<PermissionGroup> r = q.from(PermissionGroup.class);
        List<Predicate> preds=new ArrayList<>();
        addPermissionGroupsPredicate(preds,r,cb,permissionGroupsFilter);
        QueryInformationHolder<PermissionGroup> queryInformationHolder=new QueryInformationHolder<>(permissionGroupsFilter,PermissionGroup.class,securityContext);
        return getAllFiltered(queryInformationHolder,preds,cb,q,r);
    }


}
