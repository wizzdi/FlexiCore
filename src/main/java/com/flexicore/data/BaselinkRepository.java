/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;

import com.flexicore.annotations.InheritedComponent;
import com.flexicore.data.impl.BaseclassRepository;
import com.flexicore.model.*;
import com.flexicore.request.BaselinkFilter;
import com.flexicore.request.GetConnected;
import com.flexicore.request.GetDisconnected;
import com.flexicore.security.SecurityContext;


@InheritedComponent
public class BaselinkRepository extends BaseclassRepository {


	private Logger logger= Logger.getLogger(getClass().getCanonicalName());


	public void clear(){
		em.clear();
	}









	private <E extends Baseclass> List<E> getBaseclassesConnected(List<String> ids,FilteringInformationHolder like, Class<E> wanted,SecurityContext securityContext) {
		CriteriaBuilder cb=em.getCriteriaBuilder();
		CriteriaQuery<E> q=cb.createQuery(wanted);
		Root<E> r=q.from(wanted);
		List<Predicate> preds=new ArrayList<>();
		preds.add(r.get(Baseclass_.id).in(ids));
		QueryInformationHolder<E> queryInformationHolder=new QueryInformationHolder<>(like,wanted,securityContext);
		return getAllFiltered(queryInformationHolder,preds,cb,q,r);
	}

	private <E extends Baseclass> List<String> getConnectedClasses(List<String> ids,FilteringInformationHolder filteringInformationHolder, Class<E> wanted,SecurityContext securityContext) {
		CriteriaBuilder cb=em.getCriteriaBuilder();
		CriteriaQuery<String> q=cb.createQuery(String.class);
		Root<E> r=q.from(wanted);
		List<Predicate> preds=new ArrayList<>();
		preds.add(r.get(Baseclass_.id).in(ids));
		String likeClass=filteringInformationHolder.getNameLike();
		filteringInformationHolder.setNameLike(null);

		QueryInformationHolder<E> queryInformationHolder=new QueryInformationHolder<>(filteringInformationHolder,wanted,securityContext);
		prepareQuery(queryInformationHolder,preds,cb,q,r);
		if(likeClass!=null){
			preds.add(cb.like(r.get(Baseclass_.dtype),likeClass));

		}
		Predicate[] predsArray = new Predicate[preds.size()];
		predsArray = preds.toArray(predsArray);
		q.select(r.get(Baseclass_.dtype)).where(cb.and(predsArray)).orderBy(Collections.emptyList()).distinct(true);
		TypedQuery<String> query = em.createQuery(q);
		if(filteringInformationHolder.getPageSize()!=null &&filteringInformationHolder.getPageSize() > 0&& filteringInformationHolder.getCurrentPage()!=null && filteringInformationHolder.getCurrentPage() > -1){
			query.setFirstResult(filteringInformationHolder.getPageSize()*filteringInformationHolder.getCurrentPage());
			query.setMaxResults(filteringInformationHolder.getPageSize());
		}
		return query.getResultList();
	}


	private <E extends Baseclass> long countBaseclassesConnected(List<String> ids,FilteringInformationHolder like, Class<E> wanted,SecurityContext securityContext) {
		CriteriaBuilder cb=em.getCriteriaBuilder();
		CriteriaQuery<Long> q=cb.createQuery(Long.class);
		Root<E> r=q.from(wanted);
		List<Predicate> preds=new ArrayList<>();
		preds.add(r.get(Baseclass_.id).in(ids));
		QueryInformationHolder<E> queryInformationHolder=new QueryInformationHolder<>(like,wanted,securityContext);
		return countAllFiltered(queryInformationHolder,preds,cb,q,r);
	}













	public <E extends Baselink,T extends Baseclass> List<T> getConnected( GetConnected getConnected,boolean right, SecurityContext securityContext) {
		BaselinkFilter baselinkFilter = getConnected.getBaselinkFilter();
		Class<E> linkClass= (Class<E>) baselinkFilter.getLinkClass();
		Class<T> wantedClass= (Class<T>) getConnected.getWantedClass();
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<T> q = cb.createQuery(wantedClass);
		Root<T> r = q.from(wantedClass);
		Subquery<String> sub = createConnectionSubQuery(baselinkFilter, linkClass, cb, q, r,right);
		List<Predicate> preds=new ArrayList<>();
		preds.add(r.get(Baseclass_.id).in(sub));
		QueryInformationHolder<T> queryInformationHolder = new QueryInformationHolder<>(getConnected,wantedClass,securityContext);
		return getAllFiltered(queryInformationHolder,preds,cb,q,r);
	}

	public <E extends Baselink,T extends Baseclass> long countConnected( GetConnected getConnected,boolean right, SecurityContext securityContext) {
		BaselinkFilter baselinkFilter = getConnected.getBaselinkFilter();
		Class<E> linkClass= (Class<E>) baselinkFilter.getLinkClass();
		Class<T> wantedClass= (Class<T>) getConnected.getWantedClass();
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> q = cb.createQuery(Long.class);
		Root<T> r = q.from(wantedClass);
		Subquery<String> sub = createConnectionSubQuery(baselinkFilter, linkClass, cb, q, r,right);
		List<Predicate> preds=new ArrayList<>();
		preds.add(r.get(Baseclass_.id).in(sub));
		QueryInformationHolder<T> queryInformationHolder = new QueryInformationHolder<>(getConnected,wantedClass,securityContext);
		return countAllFiltered(queryInformationHolder,preds,cb,q,r);
	}

	public <E extends Baselink,T extends Baseclass> List<T> getDisconnected(GetDisconnected getDisconnected,boolean right, SecurityContext securityContext) {
		BaselinkFilter baselinkFilter = getDisconnected.getBaselinkFilter();
		Class<E> linkClass= (Class<E>) baselinkFilter.getLinkClass();
		Class<T> wantedClass= (Class<T>) getDisconnected.getWantedClass();
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<T> q = cb.createQuery(wantedClass);
		Root<T> r = q.from(wantedClass);
		Subquery<String> sub = createConnectionSubQuery(baselinkFilter, linkClass, cb, q, r,right);
		List<Predicate> preds=new ArrayList<>();
		preds.add(cb.not(r.get(Baseclass_.id).in(sub)));
		QueryInformationHolder<T> queryInformationHolder = new QueryInformationHolder<>(getDisconnected,wantedClass,securityContext);
		return getAllFiltered(queryInformationHolder,preds,cb,q,r);
	}

	public <E extends Baselink,T extends Baseclass> long countDisconnected(GetDisconnected getDisconnected,boolean right, SecurityContext securityContext) {
		BaselinkFilter baselinkFilter = getDisconnected.getBaselinkFilter();
		Class<E> linkClass= (Class<E>) baselinkFilter.getLinkClass();
		Class<T> wantedClass= (Class<T>) getDisconnected.getWantedClass();
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> q = cb.createQuery(Long.class);
		Root<T> r = q.from(wantedClass);
		Subquery<String> sub = createConnectionSubQuery(baselinkFilter, linkClass, cb, q, r,right);
		List<Predicate> preds=new ArrayList<>();
		preds.add(cb.not(r.get(Baseclass_.id).in(sub)));
		QueryInformationHolder<T> queryInformationHolder = new QueryInformationHolder<>(getDisconnected,wantedClass,securityContext);
		return countAllFiltered(queryInformationHolder,preds,cb,q,r);
	}

	public <E extends Baselink, T extends Baseclass> Subquery<String> createConnectionSubQuery(BaselinkFilter baselinkFilter, Class<E> linkClass, CriteriaBuilder cb, CriteriaQuery<?> q, Root<T> r,boolean right) {
		Subquery<String> sub=q.subquery(String.class);
		Root<E> linkRoot=sub.from(linkClass);
		Join<E,Baseclass> join=right?linkRoot.join(Baselink_.rightside):linkRoot.join(Baselink_.leftside);
		List<Predicate> subPreds=new ArrayList<>();
		addBaselinkPredicates(baselinkFilter,subPreds,linkRoot,cb);
		subPreds.add(cb.isFalse(linkRoot.get(Baselink_.softDelete)));
		Predicate[] subPredsArr=new Predicate[subPreds.size()];
		subPreds.toArray(subPredsArr);
		sub.select(join.get(Baseclass_.id)).where(subPredsArr);
		return sub;
	}




	public <T extends Baselink> List<T> getAllBaselinks(Class<T> linkClass, BaselinkFilter filter,SecurityContext securityContext) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<T> q = cb.createQuery(linkClass);
		Root<T> r = q.from(linkClass);
		List<Predicate> preds=new ArrayList<>();
		addBaselinkPredicates(filter,preds,r,cb);
		QueryInformationHolder<T> queryInformationHolder = new QueryInformationHolder<>(filter,linkClass,securityContext);
		return getAllFiltered(queryInformationHolder,preds,cb,q,r);
	}

	public <T extends Baselink> long countAllBaselinks(Class<T> linkClass, BaselinkFilter filter,SecurityContext securityContext) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> q = cb.createQuery(Long.class);
		Root<T> r = q.from(linkClass);
		List<Predicate> preds=new ArrayList<>();
		addBaselinkPredicates(filter,preds,r,cb);
		QueryInformationHolder<T> queryInformationHolder = new QueryInformationHolder<>(filter,linkClass,securityContext);
		return countAllFiltered(queryInformationHolder,preds,cb,q,r);
	}

	private <T extends Baselink> void addBaselinkPredicates(BaselinkFilter filter, List<Predicate> preds, Root<T> r, CriteriaBuilder cb) {

		if(filter.getLeftside()!=null && !filter.getLeftside().isEmpty()){
			Set<String> ids=filter.getLeftside().parallelStream().map(f->f.getId()).collect(Collectors.toSet());
			Join<T,Baseclass> join=r.join(Baselink_.leftside);
			preds.add(join.get(Baseclass_.id).in(ids));
		}

		if(filter.getRightside()!=null && !filter.getRightside().isEmpty()){
			Set<String> ids=filter.getRightside().parallelStream().map(f->f.getId()).collect(Collectors.toSet());
			Join<T,Baseclass> join=r.join(Baselink_.rightside);
			preds.add(join.get(Baseclass_.id).in(ids));
		}
		if(filter.getValue()!=null ){
			preds.add(cb.equal(r.get(Baselink_.value),filter.getValue()));
		}
		if(filter.getSimpleValue()!=null){
			preds.add(cb.equal(r.get(Baselink_.simplevalue),filter.getSimpleValue()));
		}
	}
}
