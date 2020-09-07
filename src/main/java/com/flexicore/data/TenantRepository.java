/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.data;

import java.util.ArrayList;
import java.util.List;


import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.flexicore.annotations.InheritedComponent;
import com.flexicore.data.impl.BaseclassRepository;
import com.flexicore.model.*;
import com.flexicore.request.TenantFilter;
import com.flexicore.security.SecurityContext;


@InheritedComponent
public class TenantRepository extends BaseclassRepository {
	
	public Tenant getDefaultTenant(User user){
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Tenant> q = cb.createQuery(Tenant.class);
		Root<Tenant> r = q.from(Tenant.class);
		Join<Tenant,TenantToUser> join=r.join(Tenant_.tenantToUser);
		Predicate pred=cb.equal(join.get(TenantToUser_.defualtTennant),true);
		Predicate pred2=cb.equal(join.get(TenantToUser_.rightside), user);
		List<Predicate> preds= new ArrayList<>();
		preds.add(pred);
		preds.add(pred2);
		
		finalizeQuery(r, q, preds,cb);
		TypedQuery<Tenant> query=em.createQuery(q);
		List<Tenant> list=getResultList(query);
		return list.get(0);
		
	}

	public List<TenantToUser> getAllTenants(User user){
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<TenantToUser> q = cb.createQuery(TenantToUser.class);
		Root<TenantToUser> r = q.from(TenantToUser.class);
		Predicate pred2=cb.and(cb.equal(r.get(TenantToUser_.rightside), user),cb.isFalse(r.get(TenantToUser_.softDelete)));
		List<Predicate> preds= new ArrayList<>();
		preds.add(pred2);

		finalizeQuery(r, q, preds,cb);
		TypedQuery<TenantToUser> query=em.createQuery(q);
        return getResultList(query);

	}
	

	
	public Tenant getTenantByApiKey(String apiKey){
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Tenant> q = cb.createQuery(Tenant.class);
		Root<Tenant> r = q.from(Tenant.class);
		Predicate pred=cb.equal(r.get(Tenant_.apiKey), apiKey);
		List<Predicate> preds= new ArrayList<>();
		preds.add(pred);
		finalizeQuery(r, q, preds,cb);
		TypedQuery<Tenant> query=em.createQuery(q);
		return getSingleResult(query);
	}
	

	public TenantRepository() {
		// TODO Auto-generated constructor stub
	}

	public List<Tenant> getAllTenants(TenantFilter tenantFilter, SecurityContext securityContext) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Tenant> q = cb.createQuery(Tenant.class);
		Root<Tenant> r = q.from(Tenant.class);
		List<Predicate> preds=new ArrayList<>();
		addTenantFiltering(tenantFilter, cb, r, preds);
		QueryInformationHolder<Tenant> queryInformationHolder=new QueryInformationHolder<>(tenantFilter,Tenant.class,securityContext);
		return getAllFiltered(queryInformationHolder,preds,cb,q,r);
	}

	public long countAllTenants(TenantFilter tenantFilter, SecurityContext securityContext) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> q = cb.createQuery(Long.class);
		Root<Tenant> r = q.from(Tenant.class);
		List<Predicate> preds=new ArrayList<>();
		addTenantFiltering(tenantFilter, cb, r, preds);
		QueryInformationHolder<Tenant> queryInformationHolder=new QueryInformationHolder<>(tenantFilter,Tenant.class,securityContext);
		return countAllFiltered(queryInformationHolder,preds,cb,q,r);
	}

	public static <T extends Tenant> void addTenantFiltering(TenantFilter tenantFilter, CriteriaBuilder cb, Root<T> r, List<Predicate> preds) {
		if(tenantFilter.getApiKey()!=null){
			preds.add(cb.equal(r.get(Tenant_.apiKey),tenantFilter.getApiKey()));
		}



	}
}
