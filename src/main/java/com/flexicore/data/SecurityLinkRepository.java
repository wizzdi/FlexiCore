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
import com.flexicore.request.SecurityLinkFilter;
import com.flexicore.security.SecurityContext;


import org.springframework.beans.factory.annotation.Autowired;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@InheritedComponent
public class SecurityLinkRepository extends BaseclassRepository {
 

	@Autowired
	private BaselinkRepository baselinkrepository;



	public List<SecurityLink> getAllSecurityLinks(SecurityLinkFilter securityLinkFilter, SecurityContext securityContext) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<SecurityLink> q = cb.createQuery(SecurityLink.class);
		Root<SecurityLink> r = q.from(SecurityLink.class);
		List<Predicate> preds=new ArrayList<>();
		addSecurityLinksPredicate(preds,r,cb,securityLinkFilter);
		QueryInformationHolder<SecurityLink> queryInformationHolder=new QueryInformationHolder<>(securityLinkFilter,SecurityLink.class,securityContext);
		return getAllFiltered(queryInformationHolder,preds,cb,q,r);

	}

	public long countAllSecurityLinks(SecurityLinkFilter securityLinkFilter, SecurityContext securityContext) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> q = cb.createQuery(Long.class);
		Root<SecurityLink> r = q.from(SecurityLink.class);
		List<Predicate> preds=new ArrayList<>();
		addSecurityLinksPredicate(preds,r,cb,securityLinkFilter);
		QueryInformationHolder<SecurityLink> queryInformationHolder=new QueryInformationHolder<>(securityLinkFilter,SecurityLink.class,securityContext);
		return countAllFiltered(queryInformationHolder,preds,cb,q,r);

	}

	private void addSecurityLinksPredicate(List<Predicate> preds, Root<SecurityLink> r, CriteriaBuilder cb, SecurityLinkFilter securityLinkFilter) {
		if(securityLinkFilter.getLeftsides()!=null && !securityLinkFilter.getLeftsides().isEmpty()){
			Set<String> ids=securityLinkFilter.getLeftsides().parallelStream().map(f->f.getId()).collect(Collectors.toSet());
			Join<SecurityLink,SecurityEntity> join=cb.treat(r.join(SecurityLink_.leftside),SecurityEntity.class);
			preds.add(join.get(Baseclass_.id).in(ids));
		}
	}
}
