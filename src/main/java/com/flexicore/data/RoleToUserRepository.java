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
import com.flexicore.request.RoleToUserFilter;
import com.flexicore.security.SecurityContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.ejb.Stateless;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@InheritedComponent
@Primary
public class RoleToUserRepository extends BaseclassRepository {


    public List<RoleToUser> getAllRoleToUsers(RoleToUserFilter roleToUserFilter, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<RoleToUser> q = cb.createQuery(RoleToUser.class);
        Root<RoleToUser> r = q.from(RoleToUser.class);
        List<Predicate> preds = new ArrayList<>();
        addRoleToUsersPredicate(preds, r, cb, roleToUserFilter);
        QueryInformationHolder<RoleToUser> queryInformationHolder = new QueryInformationHolder<>(roleToUserFilter, RoleToUser.class, securityContext);
        return getAllFiltered(queryInformationHolder, preds, cb, q, r);

    }

    public long countAllRoleToUsers(RoleToUserFilter roleToUserFilter, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<RoleToUser> r = q.from(RoleToUser.class);
        List<Predicate> preds = new ArrayList<>();
        addRoleToUsersPredicate(preds, r, cb, roleToUserFilter);
        QueryInformationHolder<RoleToUser> queryInformationHolder = new QueryInformationHolder<>(roleToUserFilter, RoleToUser.class, securityContext);
        return countAllFiltered(queryInformationHolder, preds, cb, q, r);

    }

    private void addRoleToUsersPredicate(List<Predicate> preds, Root<RoleToUser> r, CriteriaBuilder cb, RoleToUserFilter roleToUserFilter) {
        if (roleToUserFilter.getUsers() != null && !roleToUserFilter.getUsers().isEmpty()) {
            Set<String> ids = roleToUserFilter.getUsers().parallelStream().map(f -> f.getId()).collect(Collectors.toSet());
            Join<RoleToUser, User> join = cb.treat(r.join(RoleToUser_.rightside),User.class);
            preds.add(join.get(User_.id).in(ids));
        }

        if (roleToUserFilter.getRoles() != null && !roleToUserFilter.getRoles().isEmpty()) {
            Set<String> ids = roleToUserFilter.getRoles().parallelStream().map(f -> f.getId()).collect(Collectors.toSet());
            Join<RoleToUser, Role> join =  cb.treat(r.join(RoleToUser_.leftside),Role.class);
            preds.add(join.get(Role_.id).in(ids));
        }

    }
}
