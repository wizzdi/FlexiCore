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

import javax.ejb.Stateless;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;

import com.flexicore.annotations.InheritedComponent;
import com.flexicore.data.impl.BaseclassRepository;
import com.flexicore.model.*;
import com.flexicore.request.CategoryFilter;
import com.flexicore.request.CategoryToBaseclassFilter;
import com.flexicore.security.SecurityContext;


@InheritedComponent
public class CategoryRepository extends BaseclassRepository {

	public CategoryRepository() {
		// TODO Auto-generated constructor stub
	}

	public List<Category> getAllCategoriesAllowed(String className,
			QueryInformationHolder<Category> queryInformationHolder) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Category> q = cb.createQuery(Category.class);
		Root<Category> r = q.from(Category.class);
		Join<Category, CategoryToClazz> cctc = r.join(Category_.clazzes);
		Join<CategoryToClazz, Clazz> ctcb = cb.treat(cctc.join(CategoryToClazz_.rightside),Clazz.class);
		Predicate pred = cb.equal(ctcb.get(Baseclass_.name), className);
		List<Predicate> preds = new ArrayList<>();
		preds.add(pred);
		return getAllFiltered(queryInformationHolder, preds, cb, q, r);
	}

	public boolean isAllowed(String className, Category cat, QueryInformationHolder<Category> queryInformationHolder) {
		Clazz clazz = Baseclass.getClazzbyname(className);
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Category> q = cb.createQuery(Category.class);
		Root<Category> r = q.from(Category.class);
		Join<Category, CategoryToClazz> cctc = r.join(Category_.clazzes);
		Join<CategoryToClazz, Clazz> ctcb = cb.treat(cctc.join(CategoryToClazz_.rightside),Clazz.class);
		Predicate pred = cb.equal(ctcb.get(Baseclass_.id), clazz.getId());
		Predicate pred2 = cb.equal(r.get(Category_.id), cat.getId());
		List<Predicate> preds = new ArrayList<>();
		preds.add(pred);
		preds.add(pred2);
		List<Category> res = getAllFiltered(queryInformationHolder, preds, cb, q, r);
		return (res != null && !res.isEmpty());
	}

	public List<Category> getByName(String name, QueryInformationHolder<Category> queryInformationHolder) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Category> q = cb.createQuery(Category.class);
		Root<Category> r = q.from(Category.class);
		Predicate pred = cb.like(r.get(Category_.name), name);
		List<Predicate> preds = new ArrayList<>();
		preds.add(pred);
		return getAllFiltered(queryInformationHolder, preds, cb, q, r);
	}

	public List<CategoryToClazz> getLinksOfClazzLeftandCatRight(Clazz clazz, Category category,
			QueryInformationHolder<CategoryToClazz> queryInformationHolder) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<CategoryToClazz> q = cb.createQuery(CategoryToClazz.class);
		Root<CategoryToClazz> r = q.from(CategoryToClazz.class);
		List<Predicate> preds = new ArrayList<>();

		preds.add(cb.equal(r.get(CategoryToClazz_.leftside), category));

		preds.add(cb.equal(r.get(CategoryToClazz_.rightside), clazz));

		return getAllFiltered(queryInformationHolder, preds, cb, q, r);

	}

    public List<CategoryToBaseClass> getConnectedCategories(Baseclass base) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<CategoryToBaseClass> q = cb.createQuery(CategoryToBaseClass.class);
		Root<CategoryToBaseClass> r = q.from(CategoryToBaseClass.class);
		Predicate pred=cb.equal(r.get(CategoryToBaseClass_.rightside),base);
		q.select(r).where(pred).distinct(true);
		TypedQuery<CategoryToBaseClass> query=em.createQuery(q);
		return query.getResultList();

    }



	public boolean deleteCategoryLinks(Set<String> toRemove) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaDelete<CategoryToBaseClass> q = cb.createCriteriaDelete(CategoryToBaseClass.class);
		Root<CategoryToBaseClass> r = q.from(CategoryToBaseClass.class);
		Predicate pred=r.get(Baselink_.id).in(toRemove);
		q.where(pred);
		Query query=em.createQuery(q);
		return query.executeUpdate()> 0;
	}

	public long countAllCategories(CategoryFilter categoryFilter, SecurityContext securityContext) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> q = cb.createQuery(Long.class);
		Root<Category> r = q.from(Category.class);
		List<Predicate> preds = new ArrayList<>();
		addCategoriesPredicates(categoryFilter, r, q, cb, preds);
		QueryInformationHolder<Category> queryInformationHolder = new QueryInformationHolder<>(categoryFilter, Category.class, securityContext);
		return countAllFiltered(queryInformationHolder, preds, cb, q, r);
	}

	public List<Category> listAllCategories(CategoryFilter categoryFilter, SecurityContext securityContext) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Category> q = cb.createQuery(Category.class);
		Root<Category> r = q.from(Category.class);
		List<Predicate> preds = new ArrayList<>();
		addCategoriesPredicates(categoryFilter, r, q, cb, preds);
		QueryInformationHolder<Category> queryInformationHolder = new QueryInformationHolder<>(categoryFilter, Category.class, securityContext);
		return getAllFiltered(queryInformationHolder, preds, cb, q, r);
	}

	private void addCategoriesPredicates(CategoryFilter categoryFilter, Root<Category> r, CriteriaQuery<?> q, CriteriaBuilder cb, List<Predicate> preds) {
		if(categoryFilter.getNames()!=null && !categoryFilter.getNames().isEmpty()){
			preds.add(r.get(Category_.name).in(categoryFilter.getNames()));
		}
	}

	public List<CategoryToBaseClass> listAllCategoryToBaseclass(CategoryToBaseclassFilter categoryToBaseclassFilter, SecurityContext securityContext) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<CategoryToBaseClass> q = cb.createQuery(CategoryToBaseClass.class);
		Root<CategoryToBaseClass> r = q.from(CategoryToBaseClass.class);
		List<Predicate> preds = new ArrayList<>();
		addCategoryToBaseclassPredicates(categoryToBaseclassFilter, r, q, cb, preds);
		QueryInformationHolder<CategoryToBaseClass> queryInformationHolder = new QueryInformationHolder<>(categoryToBaseclassFilter, CategoryToBaseClass.class, securityContext);
		return getAllFiltered(queryInformationHolder, preds, cb, q, r);

	}

	private void addCategoryToBaseclassPredicates(CategoryToBaseclassFilter categoryToBaseclassFilter, Root<CategoryToBaseClass> r, CriteriaQuery<?> q, CriteriaBuilder cb, List<Predicate> preds) {


	}

	public long countAllCategoryToBaseclass(CategoryToBaseclassFilter categoryToBaseclassFilter, SecurityContext securityContext) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> q = cb.createQuery(Long.class);
		Root<CategoryToBaseClass> r = q.from(CategoryToBaseClass.class);
		List<Predicate> preds = new ArrayList<>();
		addCategoryToBaseclassPredicates(categoryToBaseclassFilter, r, q, cb, preds);
		QueryInformationHolder<CategoryToBaseClass> queryInformationHolder = new QueryInformationHolder<>(categoryToBaseclassFilter, CategoryToBaseClass.class, securityContext);
		return countAllFiltered(queryInformationHolder, preds, cb, q, r);
	}
}
