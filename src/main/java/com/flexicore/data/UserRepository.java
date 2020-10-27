/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.flexicore.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;

import com.flexicore.annotations.InheritedComponent;
import com.flexicore.data.impl.BaseclassRepository;
import com.flexicore.model.*;
import com.flexicore.request.RoleToUserFilter;
import com.flexicore.request.TenantToUserFilter;
import com.flexicore.request.UserFiltering;
import com.flexicore.security.NewUser;
import com.flexicore.security.SecurityContext;
import com.lambdaworks.crypto.SCryptUtil;

import static com.flexicore.model.User.*;


@InheritedComponent
public class UserRepository extends BaseclassRepository {



    public int multipleCreate(int number, SecurityContext securityContext) {
        NewUser rUser;
        String random = UUID.randomUUID().toString().replace("-", "");
        for (int i = 0; i < number; i++) {


            rUser = new NewUser();
            rUser.setEmail(random + i + "@test.com");
            rUser.setPassword(random);
            rUser.setPhonenumber("0522504363");
            rUser.setName("Name" + i);
            User user= new User("Name" + i, securityContext);
            user.setPassword(SCryptUtil.scrypt(rUser.getPassword(), scryptN, scryptR, scryptP));
            user.setPhoneNumber(rUser.getPhonenumber());
            user.setEmail(rUser.getEmail());
            user.setSurName(rUser.getSurname());
            em.persist(user);

        }
        return number;
    }

    public List<User> unsecureGet(int pagesize, int page) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> criteria = cb.createQuery(User.class);
        Root<User> user = criteria.from(User.class);
        TypedQuery<User> query = em.createQuery(criteria);
        setPageQuery(pagesize, page, query);
        return query.getResultList();
    }


    public User findByEmail(String email) {
        List<User> list = findByEmail(email, null);
        return list.isEmpty() ? null : list.get(0);
    }


    /**
     * find user by email
     *
     * @param email email to search users for
     * @param securityContext security context to use when searching users
     * @return users with the given emails
     */
    public List<User> findByEmail(String email, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> q = cb.createQuery(User.class);
        Root<User> r = q.from(User.class);
        Predicate pred = cb.equal(r.get(User_.email), email);
        List<Predicate> preds = new ArrayList<>();
        preds.add(pred);
        QueryInformationHolder<User> queryInformationHolder = new QueryInformationHolder<>(User.class, securityContext);
        return getAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    /**
     * add this user to the database
     * @param user user to merge
     */
    public void register(User user) {
        em.merge(user);
    }

    public void addUserToTenant(User user, Tenant tenant, SecurityContext securityContext, boolean isDefualt) {
        TenantToUser tenantToUser = new TenantToUser(tenant.getName(), securityContext);
        tenantToUser.setTenant(tenant);
        tenantToUser.setUser(user);
        tenantToUser.setDefualtTennant(isDefualt);
        tenantToUser.setTenant(tenant);
        user.getTenantToUsers().add(tenantToUser);
        em.merge(tenantToUser);
    }

    public void addUserToTenantNoPersist(User user, Tenant tenant, SecurityContext securityContext, boolean isDefualt) {
        TenantToUser tenantToUser = new TenantToUser(tenant.getName(), securityContext);
        tenantToUser.setDefualtTennant(isDefualt);
        tenantToUser.setTenant(tenant);
        tenantToUser.setUser(user);
        user.getTenantToUsers().add(tenantToUser);
    }


    public List<User> findAllOrderedByName(QueryInformationHolder<User> queryInformationHolder) {
        return getAllFiltered(queryInformationHolder);
    }

    public List<User> getUserByForgotPasswordVerificationToken(String verification) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> q = cb.createQuery(User.class);
        Root<User> r = q.from(User.class);
        q.select(r).where(cb.equal(r.get(User_.forgotPasswordToken), verification));
        TypedQuery<User> query = em.createQuery(q);
        setPageQuery(1, 0, query);
        return query.getResultList();
    }

    public List<User> getUserByEmailVerificationToken(String verification) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> q = cb.createQuery(User.class);
        Root<User> r = q.from(User.class);
        q.select(r).where(cb.equal(r.get(User_.emailVerificationToken), verification));
        TypedQuery<User> query = em.createQuery(q);
        setPageQuery(1, 0, query);
        return query.getResultList();
    }

    public List<User> findUserByPhoneNumber(String phoneNumber, SecurityContext securityContext) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> q = cb.createQuery(User.class);
        Root<User> r = q.from(User.class);
        Predicate pred = cb.equal(r.get(User_.phoneNumber), phoneNumber);
        List<Predicate> preds = new ArrayList<>();
        preds.add(pred);
        QueryInformationHolder<User> queryInformationHolder = new QueryInformationHolder<>(User.class, securityContext);
        return getAllFiltered(queryInformationHolder, preds, cb, q, r);

    }

    public List<User> getAllUsers(UserFiltering userFiltering, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> q = cb.createQuery(User.class);
        Root<User> r = q.from(User.class);
        List<Predicate> preds = new ArrayList<>();
        addUserFiltering(userFiltering, cb, r, preds);
        QueryInformationHolder<User> queryInformationHolder = new QueryInformationHolder<>(userFiltering,User.class, securityContext);
        return getAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    public long countAllUsers(UserFiltering userFiltering, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<User> r = q.from(User.class);
        List<Predicate> preds = new ArrayList<>();
        addUserFiltering(userFiltering, cb, r, preds);
        QueryInformationHolder<User> queryInformationHolder = new QueryInformationHolder<>(userFiltering,User.class, securityContext);
        return countAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    public static <T extends User> void addUserFiltering(UserFiltering userFiltering, CriteriaBuilder cb, Root<T> r, List<Predicate> preds) {

        if (userFiltering.getEmails() != null && !userFiltering.getEmails().isEmpty()) {
            preds.add(r.get(User_.email).in( userFiltering.getEmails()));
        }
        if (userFiltering.getPhoneNumbers() != null && !userFiltering.getPhoneNumbers().isEmpty()) {
            preds.add(r.get(User_.phoneNumber).in(userFiltering.getPhoneNumbers()));
        }

        if (userFiltering.getEmailVerificationToken() != null) {
            preds.add(cb.equal(r.get(User_.emailVerificationToken), userFiltering.getEmailVerificationToken()));
        }

        if (userFiltering.getForgotPasswordToken() != null) {
            preds.add(cb.equal(r.get(User_.forgotPasswordToken), userFiltering.getForgotPasswordToken()));
        }


        if (userFiltering.getLastNameLike() != null) {
            preds.add(cb.like(r.get(User_.surName), userFiltering.getLastNameLike()));
        }


        if (userFiltering.getUserIds() != null && !userFiltering.getUserIds().isEmpty()) {
            preds.add(r.get(User_.id).in(userFiltering.getUserIds()));
        }
        if (userFiltering.getUserTenants() != null && !userFiltering.getUserTenants().isEmpty()) {
            Set<String> ids = userFiltering.getUserTenants().parallelStream().map(Baseclass::getId).collect(Collectors.toSet());
            Join<T, TenantToUser> join = r.join(User_.tenantToUsers);
            Join<TenantToUser, Tenant> tenantJoin = cb.treat(join.join(Baselink_.leftside),Tenant.class);
            preds.add(tenantJoin.get(Tenant_.id).in(ids));
        }


    }

    public List<TenantToUser> getAllTenantToUsers(TenantToUserFilter tenantToUserFilter, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<TenantToUser> q = cb.createQuery(TenantToUser.class);
        Root<TenantToUser> r = q.from(TenantToUser.class);
        List<Predicate> preds = new ArrayList<>();
        addTenantToUserPredicate(tenantToUserFilter, cb, r, preds);
        QueryInformationHolder<TenantToUser> queryInformationHolder = new QueryInformationHolder<>(tenantToUserFilter,TenantToUser.class, securityContext);
        return getAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    private void addTenantToUserPredicate(TenantToUserFilter tenantToUserFilter, CriteriaBuilder cb, Root<TenantToUser> r, List<Predicate> preds) {
        if (tenantToUserFilter.getTenants() != null && !tenantToUserFilter.getTenants().isEmpty()) {
            Set<String> ids = tenantToUserFilter.getTenants().parallelStream().map(Baseclass::getId).collect(Collectors.toSet());
            Join<TenantToUser, Tenant> join = r.join(TenantToUser_.tenant);
            preds.add(join.get(Tenant_.id).in(ids));
        }

        if (tenantToUserFilter.getUsers() != null && !tenantToUserFilter.getUsers().isEmpty()) {
            Set<String> ids = tenantToUserFilter.getUsers().parallelStream().map(Baseclass::getId).collect(Collectors.toSet());
            Join<TenantToUser, User> join = cb.treat(r.join(TenantToUser_.rightside),User.class);
            preds.add(join.get(User_.id).in(ids));
        }
    }

    public long countAllTenantToUsers(TenantToUserFilter tenantToUserFilter, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<TenantToUser> r = q.from(TenantToUser.class);
        List<Predicate> preds = new ArrayList<>();
        addTenantToUserPredicate(tenantToUserFilter, cb, r, preds);
        QueryInformationHolder<TenantToUser> queryInformationHolder = new QueryInformationHolder<>(tenantToUserFilter,TenantToUser.class, securityContext);
        return countAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    public List<RoleToUser> listAllRoleToUsers(RoleToUserFilter roleToUserFilter, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<RoleToUser> q = cb.createQuery(RoleToUser.class);
        Root<RoleToUser> r = q.from(RoleToUser.class);
        List<Predicate> preds = new ArrayList<>();
        addRoleToUserPredicate(roleToUserFilter, cb, r, preds);
        QueryInformationHolder<RoleToUser> queryInformationHolder = new QueryInformationHolder<>(roleToUserFilter,RoleToUser.class, securityContext);
        return getAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    private void addRoleToUserPredicate(RoleToUserFilter roleToUserFilter, CriteriaBuilder cb, Root<RoleToUser> r, List<Predicate> preds) {

        if (roleToUserFilter.getUsers() != null && !roleToUserFilter.getUsers().isEmpty()) {
            Set<String> ids = roleToUserFilter.getUsers().parallelStream().map(Baseclass::getId).collect(Collectors.toSet());
            Join<RoleToUser, User> join = cb.treat(r.join(RoleToUser_.rightside),User.class);
            preds.add(join.get(User_.id).in(ids));
        }
    }
}
