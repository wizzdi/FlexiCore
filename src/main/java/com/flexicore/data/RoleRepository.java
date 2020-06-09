/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import org.springframework.beans.factory.annotation.Autowired;
import javax.persistence.Tuple;
import javax.persistence.criteria.*;

import com.flexicore.annotations.InheritedComponent;
import com.flexicore.data.impl.BaseclassRepository;
import com.flexicore.model.*;
import com.flexicore.request.RoleFilter;
import com.flexicore.security.SecurityContext;


@InheritedComponent
public class RoleRepository extends BaseclassRepository {
 

	@Autowired
	private BaselinkRepository baselinkrepository;


	public Role findById(String id) {
		return em.find(Role.class, id);
	}

	public List<Role> findAllOrderedByName(QueryInformationHolder<Role> queryInformationHolder) {
		return getAllFiltered(queryInformationHolder);
	}


	public List<Role> getAllUserRoles(QueryInformationHolder<Role> queryInformationHolder, User user){
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Role> q = cb.createQuery(Role.class);
		Root<Role> r = q.from(Role.class);
		Join<Role,RoleToUser> roleToUserJoin=r.join(Role_.roleToUser);
		Predicate p = cb.equal(roleToUserJoin.get(RoleToUser_.rightside),user);
		List<Predicate> preds = new ArrayList<>();
		preds.add(p);
		return getAllFiltered(queryInformationHolder,preds,cb,q,r);
	}

	
	 /*@Autowired
	    private Event<Role> roleEventSrc;*/

	    public Role register(Role role) {
	        em.persist(role);
	     //   roleEventSrc.fire(role);
	        return role;
	    }


	public List<Role> getAllRoles(RoleFilter roleFilter, SecurityContext securityContext) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Role> q = cb.createQuery(Role.class);
		Root<Role> r = q.from(Role.class);
		List<Predicate> preds=new ArrayList<>();
		addRolesPredicate(preds,r,cb,roleFilter);
		QueryInformationHolder<Role> queryInformationHolder=new QueryInformationHolder<>(roleFilter,Role.class,securityContext);
		return getAllFiltered(queryInformationHolder,preds,cb,q,r);

	}

	public long countAllRoles(RoleFilter roleFilter, SecurityContext securityContext) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> q = cb.createQuery(Long.class);
		Root<Role> r = q.from(Role.class);
		List<Predicate> preds=new ArrayList<>();
		addRolesPredicate(preds,r,cb,roleFilter);
		QueryInformationHolder<Role> queryInformationHolder=new QueryInformationHolder<>(roleFilter,Role.class,securityContext);
		return countAllFiltered(queryInformationHolder,preds,cb,q,r);

	}

	private void addRolesPredicate(List<Predicate> preds, Root<Role> r, CriteriaBuilder cb, RoleFilter roleFilter) {
	    	if(roleFilter.getUsers()!=null && !roleFilter.getUsers().isEmpty()){
				Set<String> ids=roleFilter.getUsers().parallelStream().map(f->f.getId()).collect(Collectors.toSet());
				Join<Role,RoleToUser> join=r.join(Role_.roleToUser);
				Join<RoleToUser,User> join2=cb.treat(join.join(RoleToUser_.rightside),User.class);
				preds.add(cb.and(join2.get(User_.id).in(ids),cb.isFalse(join.get(RoleToUser_.softDelete))));
			}
	    	if(roleFilter.getNames()!=null && !roleFilter.getNames().isEmpty()){
	    		preds.add(r.get(Role_.name).in(roleFilter.getNames()));
			}
	}
}
