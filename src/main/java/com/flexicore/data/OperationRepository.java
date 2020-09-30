/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.data;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.flexicore.model.dynamic.DynamicInvoker;
import com.flexicore.model.dynamic.DynamicInvoker_;
import com.flexicore.request.OperationFiltering;
import com.flexicore.security.SecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.flexicore.annotations.HasFeature;
import com.flexicore.annotations.IOperation;
import com.flexicore.annotations.InheritedComponent;
import com.flexicore.data.impl.BaseclassRepository;
import com.flexicore.model.*;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@InheritedComponent
public class OperationRepository extends BaseclassRepository {


    private static Cache<String, Operation> operationCache = CacheBuilder.newBuilder().maximumSize(50).build();


    @SuppressWarnings({"unchecked"})
    public Operation findById(String id) {
        Operation op = operationCache.getIfPresent(id);
        if (op == null) {
            op = findByIdOrNull(Operation.class, id);
            if (op != null) {
                operationCache.put(id, op);

            }
        }
        return op;
    }

    public void updateCahce(Operation operation) {
        operationCache.put(operation.getId(), operation);
    }



    public void register(Operation operation) {
        em.persist(operation);
    }

    /**
     * @param operation operation
     * @param user user
     * @param access access
     * @return if the user is allowed/denied (based on given access object) to the given operation
     */
    public boolean checkRole(Operation operation, User user, IOperation.Access access) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> q = cb.createQuery(User.class);
        Root<User> users = q.from(User.class);
        Join<User, RoleToUser> roleToUser = users.join(User_.roles, JoinType.LEFT);
        Join<RoleToUser, Role> roles = cb.treat(roleToUser.join(RoleToUser_.leftside, JoinType.LEFT),Role.class);
        Join<Role, RoleToBaseclass> roleToBaseClass = roles.join(Role_.roleToBaseclass);
        Predicate rolesPredicate = cb.and(
                cb.equal(users.get(User_.id), user.getId()),
                cb.equal(roleToBaseClass.get(RoleToBaseclass_.rightside), operation),
                cb.equal(roleToBaseClass.get(RoleToBaseclass_.simplevalue), access.name())
        );
        List<Predicate> preds = new ArrayList<>();
        preds.add(rolesPredicate);
        finalizeQuery(users, q, preds, cb);
        TypedQuery<User> query = em.createQuery(q);
        List<User> usersList = getResultList(query);
        return !usersList.isEmpty();

    }

    public boolean checkUser(Operation operation, User user, IOperation.Access access) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> q = cb.createQuery(User.class);
        Root<User> users = q.from(User.class);
        // check if this user has direct connection with the operation and the
        // value is Deny.
        Join<User, UserToBaseClass> direct = users.join(User_.userToBaseClasses, JoinType.LEFT);
        Predicate directPredicate = cb.and(cb.equal(users.get(User_.id), user.getId()),
                cb.equal(direct.get(UserToBaseClass_.rightside), operation),
                cb.equal(direct.get(UserToBaseClass_.simplevalue), access.name()));

        List<Predicate> preds = new ArrayList<>();
        preds.add(directPredicate);
        finalizeQuery(users, q, preds, cb);
        TypedQuery<User> query = em.createQuery(q);
        List<User> usersList = getResultList(query);
        return !usersList.isEmpty();
    }


    public IOperation getIOperationFromApiOperation(io.swagger.v3.oas.annotations.Operation apiOperation, Method method) {
        return new IOperation() {
            @Override
            public String Name() {
                return apiOperation.summary();
            }

            @Override
            public String Description() {
                return apiOperation.description();
            }

            @Override
            public String Category() {
                return "General";
            }

            @Override
            public boolean auditable() {
                return false;
            }

            @Override
            public Class<? extends Baseclass>[] relatedClazzes() {
                if(method.getReturnType()!=null && Baseclass.class.isAssignableFrom(method.getReturnType())){
                    return new Class[]{method.getReturnType()};
                }
                else{
                    return new Class[0];

                }
            }

            @Override
            public Access access() {
                return Access.allow;
            }

            @Override
            public HasFeature[] features() {
                return new HasFeature[0];
            }

            @Override
            public boolean noOtherLicenseRequired() {
                return false;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return IOperation.class;
            }
        };
    }

    public List<OperationToClazz> getRelatedClasses(Set<String> operationIds) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<OperationToClazz> q = cb.createQuery(OperationToClazz.class);
        Root<OperationToClazz> r = q.from(OperationToClazz.class);
        List<Predicate> preds = new ArrayList<>();
        preds.add(r.get(OperationToClazz_.leftside).in(operationIds));
        finalizeQuery(r, q, preds, cb);
        TypedQuery<OperationToClazz> query = em.createQuery(q);
        return getResultList(query);
    }
    public long countAllOperations(OperationFiltering operationFiltering, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<Operation> r = q.from(Operation.class);
        List<Predicate> preds = new ArrayList<>();
        addOperationsPredicates(operationFiltering, r, q, cb, preds);
        QueryInformationHolder<Operation> queryInformationHolder = new QueryInformationHolder<>(operationFiltering, Operation.class, securityContext);
        return countAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    public List<Operation> listAllOperations(OperationFiltering operationFiltering, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Operation> q = cb.createQuery(Operation.class);
        Root<Operation> r = q.from(Operation.class);
        List<Predicate> preds = new ArrayList<>();
        addOperationsPredicates(operationFiltering, r, q, cb, preds);
        QueryInformationHolder<Operation> queryInformationHolder = new QueryInformationHolder<>(operationFiltering, Operation.class, securityContext);
        return getAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    private void addOperationsPredicates(OperationFiltering operationFiltering, Root<Operation> r, CriteriaQuery<?> q, CriteriaBuilder cb, List<Predicate> preds) {
        if(operationFiltering.getAuditable()!=null){
            preds.add(cb.equal(r.get(Operation_.auditable),operationFiltering.getAuditable()));
        }
        if(operationFiltering.getDefaultaccess()!=null && !operationFiltering.getDefaultaccess().isEmpty()){
            Set<IOperation.Access> accesses=operationFiltering.getDefaultaccess().stream().map(f->f.getId()).collect(Collectors.toSet());
            preds.add(r.get(Operation_.defaultaccess).in(accesses));
        }
        if(operationFiltering.getDynamicInvokers()!=null && !operationFiltering.getDynamicInvokers().isEmpty()){
            Set<String> ids=operationFiltering.getDynamicInvokerIds().stream().map(f->f.getId()).collect(Collectors.toSet());
            Join<Operation, DynamicInvoker> join=r.join(Operation_.dynamicInvoker);
            preds.add(join.get(DynamicInvoker_.id).in(ids));
        }
    }
}
