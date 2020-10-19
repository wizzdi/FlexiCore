/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.data.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flexicore.annotations.Baseclassroot;
import com.flexicore.annotations.FullTextSearch;
import com.flexicore.annotations.FullTextSearchOptions;
import com.flexicore.annotations.IOperation;
import com.flexicore.annotations.rest.All;
import com.flexicore.data.jsoncontainers.BaseclassCreationContainer;
import com.flexicore.data.jsoncontainers.FieldSetContainer;
import com.flexicore.data.jsoncontainers.SortingOrder;
import com.flexicore.events.BaseclassCreated;
import com.flexicore.events.BaseclassUpdated;
import com.flexicore.model.*;
import com.flexicore.request.BaseclassCountRequest;
import com.flexicore.request.MassDeleteRequest;
import com.flexicore.response.BaseclassCount;
import com.flexicore.security.SecurityContext;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.persistence.internal.jpa.querydef.CriteriaBuilderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import javax.persistence.criteria.*;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.PluralAttribute;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Baseclassroot
@Component
public class BaseclassRepository implements com.flexicore.data.BaseclassRepository {

    /**
     *
     */
    private static final long serialVersionUID = -6837211315450751680L;
    @PersistenceContext
    protected EntityManager em;
    private Logger logger = Logger.getLogger(getClass().getCanonicalName());
    private static Operation allOp;

    @Autowired
    private ApplicationEventPublisher eventPublisher;


    @Override
    @SuppressWarnings("unchecked")

    public <T extends Baseclass> T findById(String id) {
        return (T) em.find(Baseclass.class, id);
    }

    @Override
    public <T> T findById(Class<T> type, String id) {
        return em.find(type, id);
    }

    @Override
    public <T> T findByIdOrNull(Class<T> type, String id) {
        try {
            return em.find(type, id);


        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public <T extends Baseclass> T getById(String id, Class<T> c, List<String> batchString, boolean includeSoftDelete,
                                           SecurityContext securityContext) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> q = cb.createQuery(c);
        Root<T> r = q.from(c);
        Predicate p = cb.equal(r.get(Baseclass_.id), id);
        List<Predicate> preds = new ArrayList<>();
        preds.add(p);
        QueryInformationHolder<T> info = new QueryInformationHolder<>(new FilteringInformationHolder().setFetchSoftDelete(includeSoftDelete), c, securityContext);
        info.setBatchFetchString(batchString);

        return getFiltered(info, preds, cb, q, r);

    }

    @Override
    public <T extends Baseclass> T getById(String id, Class<T> c, List<String> batchString,
                                           SecurityContext securityContext) {

        return this.getById(id, c, batchString, false, securityContext);

    }

    @Override
    public <T extends Baseclass> T getByIdOrNull(String id, Class<T> c, List<String> batchString,
                                                 SecurityContext securityContext) {
        try {
            return getById(id, c, batchString, securityContext);

        } catch (NoResultException e) {
            return null;
        }

    }

    @Override
    public <T extends Baseclass> List<T> listByNames(Class<T> c, Set<String> names, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> q = cb.createQuery(c);
        Root<T> r = q.from(c);
        List<Predicate> preds = new ArrayList<>();

        if (!names.isEmpty()) {
            Predicate predicate = r.get(Baseclass_.name).in(names);
            preds.add(predicate);
        }

        QueryInformationHolder<T> queryInformationHolder = new QueryInformationHolder<>(c, securityContext);
        return getAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    @Override
    public <T extends Baseclass> List<T> listByIds(Class<T> c, Set<String> ids, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> q = cb.createQuery(c);
        Root<T> r = q.from(c);
        List<Predicate> preds = new ArrayList<>();

        if (!ids.isEmpty()) {
            Predicate predicate = r.get(Baseclass_.id).in(ids);
            preds.add(predicate);
        }

        QueryInformationHolder<T> queryInformationHolder = new QueryInformationHolder<>(c, securityContext);
        return getAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    @Override
    public <T extends Baseclass> List<T> getByName(String name, Class<T> c, List<String> batchString,
                                                   SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> q = cb.createQuery(c);
        Root<T> r = q.from(c);
        Predicate p = cb.equal(r.get(Baseclass_.name), name);
        List<Predicate> preds = new ArrayList<>();
        preds.add(p);
        QueryInformationHolder<T> info = new QueryInformationHolder<>(c, securityContext);
        info.setBatchFetchString(batchString);
        return getAllFiltered(info, preds, cb, q, r);

    }

    @Override
    public <T extends Baseclass> T getFirstByName(String name, Class<T> c, List<String> batchString,
                                                  SecurityContext securityContext) {

        List<T> l = getByName(name, c, batchString, securityContext);
        return l.isEmpty() ? null : l.get(0);

    }


    @Override
    public <T extends Baseclass> List<T> getByNameLike(String name, Class<T> c, List<String> batchString,
                                                       SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> q = cb.createQuery(c);
        Root<T> r = q.from(c);
        Predicate p = cb.like(r.get(Baseclass_.name), name);
        List<Predicate> preds = new ArrayList<>();
        preds.add(p);
        QueryInformationHolder<T> info = new QueryInformationHolder<>(c, securityContext);
        info.setBatchFetchString(batchString);
        return getAllFiltered(info, preds, cb, q, r);

    }

    public void addtocache(Clazz clazz) {
        logger.fine("have added: " + clazz.getName() + "   class type: " + clazz.getClass().getCanonicalName());
        Baseclass.addClazz(clazz);

    }

    @Override
    @Transactional
    public boolean Persist(Baseclass baseclass) {

        try {
            em.persist(baseclass);
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while persisting entity: "
                    + baseclass.toString(), e);
        }
        return false;
    }

    @Override
    public List<String> getOrderByParameters(Class<?> clazz) {
        Field[] fields = clazz.getFields();
        List<String> SortedByFields = new ArrayList<>();
        for (Field field : fields) {

            SortedByFields.add(field.getName());

        }

        return SortedByFields;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Baseclass> List<Field> getOrderFields(Class<T> clazz) {
        List<Field> fields = new ArrayList<>();
        Field[] f = clazz.getDeclaredFields();
        for (Field field : f) {
            if (!field.isAnnotationPresent(JsonIgnore.class)) {
                fields.add(field);
            }
        }

        if (clazz.getSuperclass() != null && Baseclass.class.isAssignableFrom(clazz.getSuperclass())) {
            fields.addAll(getOrderFields((Class<T>) clazz.getSuperclass()));
        }
        return fields;
    }

    private boolean isQurifyable(Class<?> clazz, String s) {
        Field f;
        try {
            f = getFieldOninheritenceTree(clazz, s);

            if (f != null && !f.isAnnotationPresent(JsonIgnore.class)) {
                return true;
            }

        } catch (NoSuchFieldException | SecurityException e) {
            logger.log(Level.WARNING, "cant find field", e);

        }
        return false;

    }

    private Field getFieldOninheritenceTree(Class<?> cls, String fieldName) throws NoSuchFieldException {
        for (Class acls = cls; acls != null; acls = acls.getSuperclass()) {
            try {
                // if not found exception thrown
                // else return field
                return acls.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ex) {
                // ignore
            }
        }
        throw new NoSuchFieldException(fieldName);
    }


    @Override
    public List<Field> getAllComplexFields(Class<?> c) {
        List<Field> fieldsToRet = new ArrayList<>();
        for (Class acls = c; acls != null; acls = acls.getSuperclass()) {

            Field[] fields = c.getDeclaredFields();
            for (Field field : fields) {
                if (field.getClass().isAnnotationPresent(Entity.class)) {
                    fieldsToRet.add(field);
                }
            }

        }
        return fieldsToRet;
    }

    @Override
    @Transactional
    public void merge(Object base,boolean updateDate) {
        Baseclass base1=null;
        if(base instanceof Baseclass){
            OffsetDateTime now = OffsetDateTime.now();
            base1 = (Baseclass) base;
            if(updateDate){
                base1.setUpdateDate(now);
            }
            if(logger.isLoggable(Level.FINE) ){
                logger.fine("merging "+ base1.getId()+ " updateDate flag is "+updateDate +" update date "+base1.getUpdateDate());
            }
            updateSearchKey(base1);


        }

        em.merge(base);
        if(base1!=null){
            if(base1.getCreationDate()==null){
                eventPublisher.publishEvent(new BaseclassCreated<>(base1));
            }
            else{
                eventPublisher.publishEvent(new BaseclassUpdated<>(base1));

            }
        }

    }

    @Override
    public void remove(Object o) {
        em.remove(o);
    }

    @Override
    public <T extends Baseclass> void removeById(String id, Class<T> type) {
        T t = em.find(type, id);
        em.remove(t);
    }

    @Override
    public <T extends Baseclass> boolean remove(T base, Class<T> type) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete<T> delete = cb.createCriteriaDelete(type);
        Root<T> r = delete.from(type);
        delete.where(cb.equal(r.get(Baseclass_.id), base.getId()));
        Query query = em.createQuery(delete);
        int i = query.executeUpdate();

        return i > 0;

    }

    @Override
    public <T extends Baseclass> int remove(List<Predicate> preds, QueryInformationHolder<T> queryInformationHolder, CriteriaBuilder cb, CriteriaDelete<T> delete, Root<T> r) {
        if (cb == null) {
            cb = em.getCriteriaBuilder();
        }

        Class<T> type = queryInformationHolder.getType();
        if (delete == null) {
            delete = cb.createCriteriaDelete(type);
        }
        if (r == null) {
            r = delete.from(type);
        }

        prepareQuery(queryInformationHolder, preds, cb, delete, r);
        finalizeQuery(r, delete, preds, cb);
        Query query = em.createQuery(delete);
        return query.executeUpdate();

    }

    @Override
    public <T extends Baseclass> int removeById(String id, QueryInformationHolder<T> queryInformationHolder) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        Class<T> type = queryInformationHolder.getType();
        CriteriaDelete<T> delete = cb.createCriteriaDelete(type);
        Root<T> r = delete.from(type);
        Predicate p = cb.equal(r.get(Baseclass_.id), id);
        List<Predicate> preds = new ArrayList<>();
        preds.add(p);
        return remove(preds, queryInformationHolder, cb, delete, r);
    }


    @Override
    @Transactional
    public void flush() {
        em.flush();
    }


    @Override
    public <T extends Baseclass> void prepareQuery(QueryInformationHolder<T> queryInformationHolder,
                                                   List<Predicate> existingPredicates, CriteriaBuilder cb, CommonAbstractCriteria q, From<?, T> r) {
        prepareQuery(queryInformationHolder, existingPredicates, cb, q, r, false);
    }

    @Override
    public <T extends Baseclass> void prepareQuery(QueryInformationHolder<T> queryInformationHolder,
                                                   List<Predicate> existingPredicates, CriteriaBuilder cb, CommonAbstractCriteria q, From<?, T> r, boolean count) {
        prepareQuery(queryInformationHolder, existingPredicates, null, cb, q, r, count);
    }

    @Override
    public <T extends Baseclass> void prepareQuery(QueryInformationHolder<T> queryInformationHolder,
                                                   List<Predicate> existingPredicates, List<Order> orders, CriteriaBuilder cb, CommonAbstractCriteria q, From<?, T> r, boolean count) {

        Class<T> c = queryInformationHolder.getType();
        if (queryInformationHolder.getFilteringInformationHolder() == null) {
            queryInformationHolder.setFilteringInformationHolder(new FilteringInformationHolder());
        }
        List<CategoryIdFiltering> categories = queryInformationHolder.getFilteringInformationHolder().getCategories();
        List<SortParameter> sort = queryInformationHolder.getFilteringInformationHolder().getSort();
        String likeName = queryInformationHolder.getFilteringInformationHolder().getNameLike();
        String permissionContextLike = queryInformationHolder.getFilteringInformationHolder().getPermissionContextLike();

        String fullTextLike = queryInformationHolder.getFilteringInformationHolder().getFullTextLike();

        OffsetDateTime fromDate = queryInformationHolder.getFilteringInformationHolder().getFromDate();
        OffsetDateTime toDate = queryInformationHolder.getFilteringInformationHolder().getToDate();

        SecurityContext securityContext = queryInformationHolder.getSecurityContext();
        List<Tenant> tenant = securityContext != null ? securityContext.getTenants() : null;

        User user = securityContext != null ? securityContext.getUser() : null;
        boolean impersonated = securityContext != null && securityContext.isImpersonated();

        Operation operation = securityContext != null ? securityContext.getOperation() : null;
        boolean fetchSoftDelete = queryInformationHolder.getFilteringInformationHolder().isFetchSoftDelete();
        int pagesize = queryInformationHolder.getPageSize();
        int currentPage = queryInformationHolder.getCurrentPage();

        if (sort == null || sort.isEmpty()) {
            sort = new ArrayList<>();
            if (orders == null || orders.isEmpty()) {
                sort.add(new SortParameter("name", SortingOrder.ASCENDING));

            }
        }

        if (categories != null) {
            addCategoriesPredicate(existingPredicates, r, cb, categories);
        }
        List<TenantIdFiltering> tenantIds = queryInformationHolder.getFilteringInformationHolder().getTenantIds();
        if (tenantIds != null && !tenantIds.isEmpty()) {
            Set<String> tenantsIds = tenantIds.parallelStream().map(f -> f.getId()).collect(Collectors.toSet());
            addTenantsPredicate(existingPredicates, r, cb, tenantsIds);
        }
        List<ClazzIdFiltering> clazzIds = queryInformationHolder.getFilteringInformationHolder().getClazzIds();
        if (clazzIds != null && !clazzIds.isEmpty()) {
            Set<String> ids = clazzIds.parallelStream().map(f -> f.getId()).collect(Collectors.toSet());
            addClazzPredicate(existingPredicates, r, cb, ids);
        }

        if (!count && q instanceof CriteriaQuery<?>) {
            addSorted((CriteriaQuery<?>) q, r, cb, sort, orders, c);
        }
        if (fromDate != null) {
            existingPredicates.add(cb.greaterThanOrEqualTo(r.get(Baseclass_.creationDate), fromDate));
        }

        if (toDate != null) {
            existingPredicates.add(cb.lessThanOrEqualTo(r.get(Baseclass_.creationDate), toDate));

        }
        if (fullTextLike != null && !fullTextLike.isEmpty()) {
            existingPredicates.add(cb.like(cb.lower(r.get(Baseclass_.searchKey)), fullTextLike.toLowerCase()));
        }

        List<BaseclassNotIdFiltering> excludingIds = queryInformationHolder.getFilteringInformationHolder().getExcludingIds();
        if (excludingIds != null && !excludingIds.isEmpty()) {
            Set<String> ids=excludingIds.stream().map(f->f.getId()).collect(Collectors.toSet());
            existingPredicates.add(cb.not(r.get(Baseclass_.id).in(ids)));
        }

        List<BaseclassOnlyIdFiltering> onlyIds = queryInformationHolder.getFilteringInformationHolder().getOnlyIds();
        if (onlyIds != null && !onlyIds.isEmpty()) {
            Set<String> ids=onlyIds.stream().map(f->f.getId()).collect(Collectors.toSet());
            existingPredicates.add(r.get(Baseclass_.id).in(ids));
        }

        if (permissionContextLike != null && !permissionContextLike.isEmpty()) {
            existingPredicates.add(cb.like(cb.lower(r.get(Baseclass_.permissionContext)), permissionContextLike.toLowerCase()));
        }
        if (likeName != null && !likeName.isEmpty()) {
            Predicate like;
            if (queryInformationHolder.getFilteringInformationHolder().isLikeCaseSensitive()) {
                like = cb.like(r.get(Baseclass_.name), likeName);
            } else {
                like = cb.like(cb.lower(r.get(Baseclass_.name)), likeName.toLowerCase());

            }

            existingPredicates.add(like);
        }

        if (!fetchSoftDelete) {
            excludeDeleted(existingPredicates, cb, r);

        }
        if(queryInformationHolder.getFilteringInformationHolder().supportingDynamic()&&queryInformationHolder.getFilteringInformationHolder().getGenericPredicates()!=null && !queryInformationHolder.getFilteringInformationHolder().getGenericPredicates().isEmpty()&&cb instanceof CriteriaBuilderImpl){
            CriteriaBuilderImpl cbi= (CriteriaBuilderImpl) cb;
            for (Map.Entry<String, Object> predicate : queryInformationHolder.getFilteringInformationHolder().getGenericPredicates().entrySet()) {
                String key=predicate.getKey();
                Object val=predicate.getValue();
                List<Expression<String>> params= new ArrayList<>(Arrays.asList(r.get("jsonNode")));
                params.addAll(Stream.of(key.split("\\.")).map(f->cb.literal(f)).collect(Collectors.toList()));
                Expression<?>[] paramsArr=new Expression<?>[params.size()];
                params.toArray(paramsArr);
                Expression<String> jsonb_extract_path_text = cb.function("jsonb_extract_path_text", String.class, paramsArr);
                org.eclipse.persistence.expressions.Expression extract = cbi.toExpression(jsonb_extract_path_text).cast("boolean");

                Expression castedExpression = cbi.fromExpression(extract);
                Predicate pred = cb.equal(castedExpression,val);
                existingPredicates.add(pred);
            }

        }
        if (securityContext != null) {
            addSecurityNew(existingPredicates, r, cb, user, tenant, impersonated, c, operation, q);

        }

    }

    @Override
    public <T extends Baseclass> void excludeDeleted(List<Predicate> existingPredicates, CriteriaBuilder cb, From<?, T> r) {
        existingPredicates.add(cb.or(r.get(Baseclass_.softDelete).isNull(), cb.isFalse(r.get(Baseclass_.softDelete))));
    }


    /**
     * get a list of instances filtered (for now) by access control, categories,
     * keywords, paged and sorted. The method handles n+1 through EclipseLink
     * Batchfetch See <a href="https://www.eclipse.org/eclipselink/documentation/2.5/jpa/extensions/a_batchfetch.htm">eclipselink docs</a>
     * This method should be called only if there are no predicates added by the
     * caller
     *
     * @param queryInformationHolder object containing all filters , and jpa criteria objects used
     * @return list of filtered baseclasses
     */

    @Override
    public <T extends Baseclass> List<T> getAllFiltered(QueryInformationHolder<T> queryInformationHolder) {

        return getAllFiltered(queryInformationHolder, new ArrayList<>(), null, null, null);
    }

    @Override
    public <T extends Baseclass> long countAllFiltered(QueryInformationHolder<T> queryInformationHolder) {
        return countAllFiltered(queryInformationHolder, new ArrayList<>(), null, null, null);
    }

    @Override
    public <T extends Baseclass> long countAllFiltered(QueryInformationHolder<T> queryInformationHolder,
                                                       List<Predicate> existingPredicates, CriteriaBuilder cb, CriteriaQuery<Long> q, Root<T> r) {
        Class<T> c = queryInformationHolder.getType();
        if (cb == null) {
            cb = em.getCriteriaBuilder();
        }
        if (q == null) {
            q = cb.createQuery(Long.class);
        }
        if (r == null) {
            r = q.from(c);
        }

        if (existingPredicates == null) {
            existingPredicates = new ArrayList<>();
        }
        prepareQuery(queryInformationHolder, existingPredicates, cb, q, r, true);
        finalizeCountQuery(r, q, existingPredicates, cb);
        TypedQuery<Long> query = em.createQuery(q);

        return getSingleResult(query);
    }

    /**
     * get a list of instances filtered (for now) by access control, categories,
     * keywords, paged and sorted. The method handles n+1 through EclipseLink
     * Batchfetch See <a href="https://www.eclipse.org/eclipselink/documentation/2.5/jpa/extensions/a_batchfetch.htm">eclipselink docs</a>
     * This method should be called if there are predicates added by the caller
     * , all predicate are 'AND'
     *
     * @param queryInformationHolder object containing all filters , and jpa criteria objects used
     * @param existingPredicates epredicates
     * @param cb criteria builder
     * @param q query
     * @param r root
     * @return filtered baseclass
     */
    @Override
    public <T extends Baseclass> List<T> getAllFiltered(QueryInformationHolder<T> queryInformationHolder,
                                                        List<Predicate> existingPredicates, CriteriaBuilder cb, CriteriaQuery<T> q, From<?, T> r) {
        return getAllFiltered(queryInformationHolder, existingPredicates, cb, q, r, queryInformationHolder.getType(), r);
    }

    @Override
    public <T extends Baseclass> List<T> getAllFiltered(QueryInformationHolder<T> queryInformationHolder,
                                                        List<Predicate> existingPredicates, List<Order> orders, CriteriaBuilder cb, CriteriaQuery<T> q, From<?, T> r) {
        return getAllFiltered(queryInformationHolder, existingPredicates, orders, cb, q, r, queryInformationHolder.getType(), r);
    }


    /**
     * get a list of instances filtered (for now) by access control, categories,
     * keywords, paged and sorted. The method handles n+1 through EclipseLink
     * Batchfetch See <a href="https://www.eclipse.org/eclipselink/documentation/2.5/jpa/extensions/a_batchfetch.htm">eclipselink docs</a>
     * This method should be called if there are predicates added by the caller
     * , all predicate are 'AND'
     *
     * @param queryInformationHolder object containing all filters , and jpa criteria objects used
     * @param existingPredicates predicates
     * @param cb criteria builder
     * @param q query
     * @param r root
     * @return list of filtered baseclasses
     */
    @Override
    public <T extends Baseclass, E> List<E> getAllFiltered(QueryInformationHolder<T> queryInformationHolder,
                                                           List<Predicate> existingPredicates, CriteriaBuilder cb, CriteriaQuery<E> q, From<?, T> r, Class<E> selectionClass, Selection<? extends E> select) {
        return getAllFiltered(queryInformationHolder, existingPredicates, null, cb, q, r, selectionClass, select);
    }

    @Override
    public <T extends Baseclass, E> List<E> getAllFiltered(QueryInformationHolder<T> queryInformationHolder,
                                                           List<Predicate> existingPredicates, List<Order> orders, CriteriaBuilder cb, CriteriaQuery<E> q, From<?, T> r, Class<E> selectionClass, Selection<? extends E> select) {
        Class<T> c = queryInformationHolder.getType();
        if (cb == null) {
            cb = em.getCriteriaBuilder();
        }
        if (q == null) {
            q = cb.createQuery(selectionClass);
        }
        if (r == null) {
            r = q.from(c);

        }
        if (select == null) {
            select = (Selection<? extends E>) r;
        }
        if (existingPredicates == null) {
            existingPredicates = new ArrayList<>();
        }
        if (orders == null) {
            orders = new ArrayList<>();
        }
        prepareQuery(queryInformationHolder, existingPredicates, cb, q, r);
        int pagesize = queryInformationHolder.getPageSize();
        int currentPage = queryInformationHolder.getCurrentPage();
        finalizeQuery(r, q, existingPredicates, cb, select);
       // q.getGroupList().add(r.get(Baseclass_.id));

        TypedQuery<E> query = em.createQuery(q);

        setBatchFetch(query, queryInformationHolder.getBatchFetchString());
        if (pagesize != -1) {
            setPageQuery(pagesize, currentPage, query);
        }
        return getResultList(query);

    }


    @Override
    public <T extends Baseclass> T getFiltered(QueryInformationHolder<T> queryInformationHolder,
                                               List<Predicate> existingPredicates) {
        return getFiltered(queryInformationHolder, existingPredicates, null, null, null);
    }

    @Override
    public <T extends Baseclass> T getFiltered(QueryInformationHolder<T> queryInformationHolder,
                                               List<Predicate> existingPredicates, CriteriaBuilder cb, CriteriaQuery<T> q, Root<T> r) {
        Class<T> c = queryInformationHolder.getType();
        if (cb == null) {
            cb = em.getCriteriaBuilder();
        }
        if (q == null) {
            q = cb.createQuery(c);
        }
        if (r == null) {
            r = q.from(c);
        }

        if (existingPredicates == null) {
            existingPredicates = new ArrayList<>();
        }
        prepareQuery(queryInformationHolder, existingPredicates, cb, q, r);
        int pagesize = queryInformationHolder.getPageSize();
        int currentPage = queryInformationHolder.getCurrentPage();
        finalizeQuery(r, q, existingPredicates, cb);
        TypedQuery<T> query = em.createQuery(q);
        setBatchFetch(query, queryInformationHolder.getBatchFetchString());

        if (pagesize > 0) {
            setPageQuery(pagesize, currentPage, query);
        }
        return getSingleResult(query);
    }

    @Override
    public <T extends Baseclass, E> E getFiltered(QueryInformationHolder<T> queryInformationHolder,
                                                  List<Predicate> existingPredicates, CriteriaBuilder cb, CriteriaQuery<E> q, Root<T> r, Class<E> selectionClass, Selection<? extends E> select) {
        Class<T> c = queryInformationHolder.getType();
        if (cb == null) {
            cb = em.getCriteriaBuilder();
        }
        if (q == null) {
            q = cb.createQuery(selectionClass);
        }
        if (r == null) {
            r = q.from(c);
        }

        if (existingPredicates == null) {
            existingPredicates = new ArrayList<>();
        }
        prepareQuery(queryInformationHolder, existingPredicates, cb, q, r);
        int pagesize = queryInformationHolder.getPageSize();
        int currentPage = queryInformationHolder.getCurrentPage();
        finalizeQuery(r, q, existingPredicates, cb, select);
        TypedQuery<E> query = em.createQuery(q);
        setBatchFetch(query, queryInformationHolder.getBatchFetchString());

        if (pagesize > 0) {
            setPageQuery(pagesize, currentPage, query);
        }
        return getSingleResult(query);
    }

    /**
     * adds hints for batch fetch (this is Eclipselink specific!, solves n+1
     * syndrome)
     *
     * @param query query
     * @param list list
     */
    private <T extends Baseclass> void setBatchFetch(TypedQuery<?> query, List<String> list) {
        if (list != null) {
            for (String string : list) {
                query.setHint("eclipselink.batch", string);
            }

            query.setHint("eclipselink.batch.type", "IN");
        }

    }

    /**
     * adds all predicates to the query
     *
     * @param r root
     * @param q query
     * @param preds predicates
     * @param cb criteria builder
     */
    @Override
    public <T> void finalizeQuery(From<?, T> r, CriteriaQuery<T> q, List<Predicate> preds, CriteriaBuilder cb) {
        finalizeQuery(r, q, preds, cb, r);

    }

    /**
     * adds all predicates to the query
     *
     * @param r root
     * @param q query
     * @param preds predicates
     * @param cb criteria builder
     */
    @Override
    public <T> void finalizeQuery(From<?, T> r, CriteriaDelete<T> q, List<Predicate> preds, CriteriaBuilder cb) {
        finalizeQuery(r, q, preds, cb, r);

    }

    /**
     * adds all predicates to the query
     *
     * @param r root
     * @param q query
     * @param preds predicates
     * @param cb criteria builder
     */
    @Override
    public <T, E> void finalizeQuery(From<?, T> r, CriteriaQuery<E> q, List<Predicate> preds, CriteriaBuilder cb, Selection<? extends E> select) {
        if (preds != null && !preds.isEmpty()) {
            Predicate[] predsArray = new Predicate[preds.size()];
            predsArray = preds.toArray(predsArray);
            q.select(select).where(cb.and(predsArray)).distinct(true);


        } else {
            q.select(select);
        }

    }

    /**
     * adds all predicates to the query
     *
     * @param r root
     * @param q query
     * @param preds predicates
     * @param cb criteria builder
     */
    @Override
    public <T, E> void finalizeQuery(From<?, T> r, CriteriaDelete<E> q, List<Predicate> preds, CriteriaBuilder cb, Selection<? extends E> select) {
        Predicate[] predsArray = new Predicate[preds.size()];
        predsArray = preds.toArray(predsArray);
        q.where(cb.and(predsArray));


    }

    @Override
    public <T> void finalizeCountQuery(From<?, T> r, CriteriaQuery<Long> q, List<Predicate> preds, CriteriaBuilder cb) {
        finalizeQuery(r, q, preds, cb, cb.countDistinct(r));

    }

    @Override
    public <T> T getSingleResult(TypedQuery<T> query) {
        return query.getSingleResult();
    }

    @Override
    public <T> T getSingleResultOrNull(TypedQuery<T> query) {
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }

    }

    @Override
    public <T> List<T> getResultList(TypedQuery<T> query) {
        return query.getResultList();
    }

    @Override
    public void setPageQuery(int pagesize, int currentPage, TypedQuery<?> query) {
        query.setFirstResult(currentPage * pagesize);
        query.setMaxResults(pagesize);
    }

    @Override
    public <T> void addSorted(CriteriaQuery<?> q, From<?, T> r, CriteriaBuilder cb, List<SortParameter> sort, Class<T> c) {
        addSorted(q, r, cb, sort, null, c);
    }


    @Override
    public <T> void addSorted(CriteriaQuery<?> q, From<?, T> r, CriteriaBuilder cb, List<SortParameter> sort, List<Order> orders, Class<T> c) {
        List<Order> orderby = new ArrayList<>();
        if (orders != null) {
            for (Order order : orders) {
                q.orderBy(order);
            }
        }

        if (sort != null && !sort.isEmpty()) {
            Order o;
            for (SortParameter sortparameter : sort) {
                if (isQurifyable(c, sortparameter.getName())) {
                    if (sortparameter.getSortingOrder().equals(SortingOrder.ASCENDING)) {
                        o = cb.asc(r.get(sortparameter.getName()));
                    } else {
                        o = cb.desc(r.get(sortparameter.getName()));

                    }

                    if (o != null) {
                        orderby.add(o);
                    }
                } else {
                    logger.log(Level.WARNING, sortparameter.getName() + " is not qurifyable");
                }

            }
            if (!orderby.isEmpty()) {
                q.orderBy(orderby);
            }

        }

    }


    private boolean isSuperAdmin(List<Role> roles) {
        for (Role role : roles) {
            if (role.getId().equals("HzFnw-nVR0Olq6WBvwKcQg")) {
                return true;
            }

        }
        return false;

    }

   /* private static class SecurityConnectionFilter{
        Baseclass leftside;
        Set<String> leftsideIds;
        IOperation.Access access;
        Operation op;

        public Baseclass getLeftside() {
            return leftside;
        }

        public <T extends SecurityConnectionFilter> T setLeftside(Baseclass leftside) {
            this.leftside = leftside;
            return (T) this;
        }

        public Set<String> getLeftsideIds() {
            return leftsideIds;
        }

        public <T extends SecurityConnectionFilter> T setLeftsideIds(Set<String> leftsideIds) {
            this.leftsideIds = leftsideIds;
            return (T) this;
        }

        public IOperation.Access getAccess() {
            return access;
        }

        public <T extends SecurityConnectionFilter> T setAccess(IOperation.Access access) {
            this.access = access;
            return (T) this;
        }

        public Operation getOp() {
            return op;
        }

        public <T extends SecurityConnectionFilter> T setOp(Operation op) {
            this.op = op;
            return (T) this;
        }
    }*/

   /* private <E extends SecurityLink> Subquery<String> getSecurityConnectionSubQuery(Class<E> c,SecurityConnectionFilter securityConnectionFilter,
                                                                                                         CommonAbstractCriteria query,
                                                                                                         CriteriaBuilder cb) {
        Subquery<String> subquery = query.subquery(String.class);
        Root<E> r = subquery.from(c);
        Join<E, Baseclass> b = r.join(Baselink_.rightside);

        Predicate predicate = cb.and(cb.isFalse(b.get(Baseclass_.softDelete)));
        if(securityConnectionFilter.leftside!=null){
            predicate=cb.and(predicate,cb.equal(r.get(Baselink_.leftside), securityConnectionFilter.leftside));

        }
        if(securityConnectionFilter.leftsideIds!=null && !securityConnectionFilter.leftsideIds.isEmpty()){
            Join<E,Baseclass> leftsideJoin=r.join(Baselink_.leftside);
            predicate=cb.and(predicate,leftsideJoin.get(Baseclass_.id).in(securityConnectionFilter.leftsideIds));

        }
        if(securityConnectionFilter.op!=null){
            predicate=cb.and(predicate,cb.or(cb.equal(r.get(Baselink_.value), securityConnectionFilter.op), cb.equal(r.get(Baselink_.value), allOp)));

        }
        if(securityConnectionFilter.access!=null){
            predicate=cb.and(predicate,cb.equal(r.get(Baselink_.simplevalue),securityConnectionFilter.access.name()));
        }
        return subquery.select(b.get(Baseclass_.id))
                .where(
                        predicate
                );
    }*/
/*
    private <T extends Baseclass> void addSecurityV2(List<Predicate> existingPredicates, From<?, T> r, CriteriaBuilder cb,
                                                     User user, List<Tenant> tenants, boolean impersonated, Class<T> c, Operation op, CommonAbstractCriteria query) {

        Map<String, Tenant> tenantsMap = tenants.parallelStream().collect(Collectors.toMap(f -> f.getId(), f -> f, (a, b) -> a));
        List<Role> roles = getAllRoles(user).parallelStream().filter(f -> f.getTenant() != null && tenantsMap.containsKey(f.getTenant().getId())).collect(Collectors.toList());
        if (isSuperAdmin(roles)) {
            return;
        }
        Map<String, List<Role>> rolesInTenants = roles.parallelStream().filter(f -> f.getTenant() != null).collect(Collectors.groupingBy(f -> f.getTenant().getId()));
        Subquery<String> userDeniedBaseclass = getSecurityConnectionSubQuery(UserToBaseClass.class,new SecurityConnectionFilter().setAccess(IOperation.Access.deny).setLeftside(user).setOp(op), query,cb);
        Subquery<String> roleDeniedBaseclass = getSecurityConnectionSubQuery(RoleToBaseclass.class,new SecurityConnectionFilter().setAccess(IOperation.Access.deny).setLeftsideIds(roles.parallelStream().map(f->f.getId()).collect(Collectors.toSet())).setOp(op), query,cb);
        Subquery<String> tenantDeniedBaseclass = getSecurityConnectionSubQuery(TenantToBaseClassPremission.class,new SecurityConnectionFilter().setAccess(IOperation.Access.deny).setLeftsideIds(tenants.parallelStream().map(f->f.getId()).collect(Collectors.toSet())).setOp(op), query,cb);

        Subquery<String> userAllowedBaseclass = getSecurityConnectionSubQuery(UserToBaseClass.class,new SecurityConnectionFilter().setAccess(IOperation.Access.allow).setLeftside(user).setOp(op), query,cb);
        Map<String, Subquery<String>> rolesAllowedBaseclass = rolesInTenants.entrySet().parallelStream().collect(Collectors.toMap(e -> e.getKey(), e -> getSecurityConnectionSubQuery(RoleToBaseclass.class,new SecurityConnectionFilter().setAccess(IOperation.Access.allow).setLeftsideIds(e.getValue().parallelStream().map(f->f.getId()).collect(Collectors.toSet())).setOp(op), query,cb)));
        Map<String, Subquery<String>> tenantsAllowedBaseclass = tenantsMap.values().parallelStream().collect(Collectors.toMap(tenant -> tenant.getId(), tenant -> getSecurityConnectionSubQuery(TenantToBaseClassPremission.class,new SecurityConnectionFilter().setAccess(IOperation.Access.allow).setLeftside(tenant).setOp(op), query,cb)));

        Predicate predicate = cb.or(
                cb.equal(r.get(Baseclass_.creator), user),//creator
                cb.and(cb.isNull(r.get(Baseclass_.creator)), r.get(Baseclass_.tenant).in(tenants)),
                r.get(Baseclass_.id).in(userAllowedBaseclass),//user is allowed to the baseclass specifiably
                cb.and(r.get(Baseclass_.clazz).get(Baseclass_.id).in(userAllowedBaseclass), cb.not(r.get(Baseclass_.id).in(userDeniedBaseclass, roleDeniedBaseclass, tenantDeniedBaseclass))),//user is allowed to the clazz but is not denied to the specific instance
        );
        Predicate rolesBaseclassPredicate = cb.or();
        for (Subquery<String> subquery : rolesAllowedBaseclass.values()) {
            rolesBaseclassPredicate = cb.or(rolesBaseclassPredicate, r.get(Baseclass_.id).in(subquery));//role is allowed to the baseclass specifiably but not denied for user

        }
        rolesBaseclassPredicate = cb.and(rolesBaseclassPredicate, cb.not(r.get(Baseclass_.id).in(userDeniedBaseclass)));
        predicate = cb.or(predicate, rolesBaseclassPredicate);
        Predicate tenantsBaseclassPredicate = cb.or();
        for (Subquery<String> subquery : tenantsAllowedBaseclass.values()) {
            tenantsBaseclassPredicate = cb.or(tenantsBaseclassPredicate, r.get(Baseclass_.id).in(subquery));//role is allowed to the baseclass specifiably but not denied for user

        }
        tenantsBaseclassPredicate = cb.and(tenantsBaseclassPredicate, cb.not(r.get(Baseclass_.id).in(userDeniedBaseclass, roleDeniedBaseclass)));
        predicate = cb.or(predicate, tenantsBaseclassPredicate);
        Predicate rolesClazzPredicate = cb.or();
        for (Map.Entry<String, Subquery<String>> entry : rolesAllowedBaseclass.entrySet()) {
            rolesClazzPredicate = cb.or(
                    rolesClazzPredicate,
                    cb.and(cb.equal(r.get(Baseclass_.tenant).get(Baseclass_.id), entry.getKey()), r.get(Baseclass_.clazz).get(Baseclass_.id).in(entry.getValue())));//role is allowed to the baseclass specifiably but not denied for user

        }
        rolesClazzPredicate = cb.and(rolesClazzPredicate, cb.not(r.get(Baseclass_.clazz).in(userDeniedBaseclass)), cb.not(r.get(Baseclass_.id).in(userDeniedBaseclass, roleDeniedBaseclass, tenantDeniedBaseclass)));
        predicate = cb.or(predicate, rolesClazzPredicate);

        Predicate tenantsClazzPredicate = cb.or();
        for (Map.Entry<String, Subquery<String>> entry : tenantsAllowedBaseclass.entrySet()) {
            tenantsClazzPredicate = cb.or(
                    tenantsClazzPredicate,
                    cb.and(cb.equal(r.get(Baseclass_.tenant).get(Baseclass_.id), entry.getKey()), r.get(Baseclass_.clazz).get(Baseclass_.id).in(entry.getValue())));//role is allowed to the baseclass specifiably but not denied for user

        }
        tenantsClazzPredicate = cb.and(tenantsClazzPredicate, cb.not(r.get(Baseclass_.clazz).in(userDeniedBaseclass, roleDeniedBaseclass)), cb.not(r.get(Baseclass_.id).in(userDeniedBaseclass, roleDeniedBaseclass, tenantDeniedBaseclass)));
        predicate = cb.or(predicate, tenantsClazzPredicate);

        existingPredicates.add(predicate);


    }
*/

    private <T extends Baseclass> void addSecurityNew(List<Predicate> existingPredicates, From<?, T> r, CriteriaBuilder cb,
                                                      User user, List<Tenant> tenants, boolean impersonated, Class<T> c, Operation op, CommonAbstractCriteria query) {
        Set<String> tenantIds = tenants.parallelStream().map(f -> f.getId()).collect(Collectors.toSet());
        List<Role> roles = getAllRoles(user).parallelStream().filter(f -> f.getTenant() != null && tenantIds.contains(f.getTenant().getId())).collect(Collectors.toList());
        if (isSuperAdmin(roles)) {
            return;
        }
        Map<String, List<Role>> rolesInTenants = roles.parallelStream().filter(f -> f.getTenant() != null).collect(Collectors.groupingBy(f -> f.getTenant().getId()));
        Pair<List<Baseclass>, List<Baseclass>> denied = getDenied(user, op);
        List<Baseclass> userDenied = denied.getLeft();
        List<Baseclass> roleDenied = denied.getRight();
        roleDenied.addAll(userDenied);


        Clazz clazz = Baseclass.getClazzbyname(c.getCanonicalName());
        Subquery<String> sub = getBaseclassSpecificSubQeury(query, cb, roles, user, tenants, op, clazz, userDenied, roleDenied);
        Subquery<String> subPermissionGroup = getPermissionGroupSubQuery(query, cb, roles, user, tenants, op, clazz, userDenied, roleDenied);

        Predicate premissive = cb.or();
        Predicate all = cb.or();
        //check for allow all links for tenants or user - if link is for user grant permission for all objects in all tenants , if allow link
        List<SecurityLink> hasAllLink = getAllowAllLinks(cb, roles, user, tenants, op);
        for (SecurityLink securityLink : hasAllLink) {
            Predicate mid = cb.or();
            if (!tenants.isEmpty() && securityLink.getLeftside() instanceof User) {
                mid = r.get(Baseclass_.tenant).in(tenants);
            } else {
                if (securityLink.getLeftside() instanceof Tenant) {
                    mid = cb.equal(r.get(Baseclass_.tenant), securityLink.getLeftside());
                } else {
                    if (securityLink.getLeftside() instanceof Role) {
                        Role role = (Role) securityLink.getLeftside();
                        if (role.getTenant() != null && rolesInTenants.get(role.getTenant().getId()) != null) {
                            mid = cb.equal(r.get(Baseclass_.tenant), role.getTenant());

                        }
                    }
                }
            }
            if (!roleDenied.isEmpty()) {
                mid = cb.and(mid, cb.not(r.in(roleDenied)));
            }
            premissive = cb.or(premissive, mid);
        }

        Subquery<String> subPremissive = getPremissiveSubQueryForTenantAndUser(query, cb, user, tenants, op);
        premissive = cb.or(
                premissive,
                cb.and(r.get(Baseclass_.clazz).get(Clazz_.id).in(subPremissive),
                        r.get(Baseclass_.tenant).in(tenants)));


        Map<String, Tenant> tenantMap = tenants.parallelStream().collect(Collectors.toMap(f -> f.getId(), f -> f, (a, b) -> a));
        for (Map.Entry<String, List<Role>> entry : rolesInTenants.entrySet()) {
            Tenant tenant = tenantMap.get(entry.getKey());
            if (tenant != null) {
                Subquery<String> subPremissiveRole = getPremissiveSubQueryForRole(query, cb, entry.getValue(), op);
                premissive = cb.or(premissive, cb.and(r.get(Baseclass_.clazz).get(Clazz_.id).in(subPremissiveRole), cb.equal(r.get(Baseclass_.tenant), tenant)));


            }

        }


        Predicate creatorPred = cb.equal(r.get(Baseclass_.creator), user);
        if (impersonated) {
            creatorPred = cb.and(creatorPred, r.get(Baseclass_.tenant).in(tenants));
        }
        Predicate predicate = cb.or(

                r.get(Baseclass_.id).in(sub),
                r.get(Baseclass_.id).in(subPermissionGroup),
                premissive,
                all,


                //creator
                creatorPred,
                //enforce tenancy on Premissive Creator value
                cb.and(cb.isNull(r.get(Baseclass_.creator)), r.get(Baseclass_.tenant).in(tenants))


        );


        List<Predicate> ors = new ArrayList<>();
        ors.add(predicate);


        //ors.add(predicate);

        Predicate[] preds = new Predicate[ors.size()];
        preds = ors.toArray(preds);
        Predicate securityPredicates = cb.or(preds);
        if (existingPredicates == null) {
            existingPredicates = new ArrayList<>();
        }
        existingPredicates.add(securityPredicates);

    }

    private Subquery<String> getPermissionGroupSubQuery(CommonAbstractCriteria query, CriteriaBuilder cb, List<Role> roles, User user, List<Tenant> tenants, Operation op, Clazz clazz, List<Baseclass> userDenied, List<Baseclass> roleDenied) {
        Subquery<String> sub = query.subquery(String.class);
        Root<SecurityLink> securityLinkRoot = sub.from(SecurityLink.class);
        Join<SecurityLink, PermissionGroup> rightsideJoin = cb.treat(securityLinkRoot.join(SecurityLink_.rightside), PermissionGroup.class);
        Root<UserToBaseClass> userToBaseClassRoot = cb.treat(securityLinkRoot, UserToBaseClass.class);
        Root<RoleToBaseclass> roleToBaseclassRoot = cb.treat(securityLinkRoot, RoleToBaseclass.class);
        Root<TenantToBaseClassPremission> tenantToBaseClassPremissionRoot = cb.treat(securityLinkRoot, TenantToBaseClassPremission.class);
        Join<PermissionGroup, PermissionGroupToBaseclass> permissionGroupLinkJoin = rightsideJoin.join(PermissionGroup_.links);
        Join<PermissionGroupToBaseclass, Baseclass> permissionGroupTargetJoin = permissionGroupLinkJoin.join(PermissionGroupToBaseclass_.rightside);


        Predicate linkPredicate = cb.and(
                createBaseclassSpecificPredicate(cb, roles, user, tenants, op, userDenied, roleDenied, securityLinkRoot, userToBaseClassRoot, roleToBaseclassRoot, tenantToBaseClassPremissionRoot),
                cb.isFalse(permissionGroupLinkJoin.get(PermissionGroupToBaseclass_.softDelete)),
                cb.isFalse(rightsideJoin.get(PermissionGroup_.softDelete))
        );

        sub.select(permissionGroupTargetJoin.get(Baseclass_.id)).where(linkPredicate);
        return sub;

    }

    private Subquery<String> getBaseclassSpecificSubQeury(CommonAbstractCriteria query, CriteriaBuilder cb, List<Role> roles, User user
            , List<Tenant> tenants, Operation op, Clazz clazz, List<Baseclass> userDenied, List<Baseclass> roleDenied) {
        Subquery<String> sub = query.subquery(String.class);
        Root<SecurityLink> securityLinkRoot = sub.from(SecurityLink.class);
        Join<SecurityLink, Baseclass> rightsideJoin = securityLinkRoot.join(SecurityLink_.rightside);
        Root<UserToBaseClass> userToBaseClassRoot = cb.treat(securityLinkRoot, UserToBaseClass.class);
        Root<RoleToBaseclass> roleToBaseclassRoot = cb.treat(securityLinkRoot, RoleToBaseclass.class);
        Root<TenantToBaseClassPremission> tenantToBaseClassPremissionRoot = cb.treat(securityLinkRoot, TenantToBaseClassPremission.class);


        Predicate linkPredicate = createBaseclassSpecificPredicate(cb, roles, user, tenants, op, userDenied, roleDenied, securityLinkRoot, userToBaseClassRoot, roleToBaseclassRoot, tenantToBaseClassPremissionRoot);
        sub.select(rightsideJoin.get(Baseclass_.id)).where(linkPredicate);
        return sub;

    }

    private Predicate createBaseclassSpecificPredicate(CriteriaBuilder cb, List<Role> roles, User user, List<Tenant> tenants, Operation op, List<Baseclass> userDenied, List<Baseclass> roleDenied, Root<SecurityLink> securityLinkRoot, Root<UserToBaseClass> userToBaseClassRoot, Root<RoleToBaseclass> roleToBaseclassRoot, Root<TenantToBaseClassPremission> tenantToBaseClassPremissionRoot) {
        Operation allOpId = getAllOperation();

        Predicate rolesPredicate = cb.or();
        if (!roles.isEmpty()) {
            rolesPredicate = cb.and(
                    roleToBaseclassRoot.get(RoleToBaseclass_.leftside).in(roles));

        }


        Predicate userPredicate = cb.and(
                cb.equal(userToBaseClassRoot.get(UserToBaseClass_.leftside), user));

        Predicate tenantPredicate = cb.and(
                tenantToBaseClassPremissionRoot.get(TenantToBaseClassPremission_.leftside).in(tenants));


        if (!userDenied.isEmpty()) {
            userPredicate = cb.and(userPredicate, cb.not(securityLinkRoot.get(SecurityLink_.rightside).in(userDenied)));
        }


        if (!roleDenied.isEmpty()) {
            rolesPredicate = cb.and(rolesPredicate, cb.not(securityLinkRoot.get(SecurityLink_.rightside).in(roleDenied)));
            tenantPredicate = cb.and(tenantPredicate, cb.not(securityLinkRoot.get(SecurityLink_.rightside).in(roleDenied)));
        }

        return cb.and(
                cb.or(cb.isFalse(securityLinkRoot.get(SecurityLink_.softDelete)), securityLinkRoot.get(SecurityLink_.softDelete).isNull()),
                cb.or(cb.equal(securityLinkRoot.get(SecurityLink_.value), op), cb.equal(securityLinkRoot.get(SecurityLink_.value), allOpId)),
                //cb.or(cb.equal(securityLinkRoot.get(SecurityLink_.rightside).get(Baseclass_.clazz),clazz)),
                cb.equal(securityLinkRoot.get(SecurityLink_.simplevalue), IOperation.Access.allow.name()),
                cb.or(
                        userPredicate,
                        rolesPredicate,
                        tenantPredicate


                )


        );
    }

    private Operation getAllOperation() {
        if (allOp == null) {
            allOp = findById(Baseclass.generateUUIDFromString(All.class.getCanonicalName()));
        }
        return allOp;
    }

    private Subquery<String> getPremissiveSubQueryForRole(CommonAbstractCriteria query, CriteriaBuilder cb, List<Role> roles, Operation op) {

        Subquery<String> subPremissive = query.subquery(String.class);
        Root<SecurityLink> securityLinkRootPremissive = subPremissive.from(SecurityLink.class);
        Join<SecurityLink, Clazz> rightside = cb.treat(securityLinkRootPremissive.join(SecurityLink_.rightside), Clazz.class);
        Root<RoleToBaseclass> roleToBaseclassRoot = cb.treat(securityLinkRootPremissive, RoleToBaseclass.class);
        Operation allOpId = getAllOperation();
        Predicate rolesPredicatePremissive = cb.or();
        if (!roles.isEmpty()) {
            rolesPredicatePremissive = cb.and(
                    roleToBaseclassRoot.get(RoleToBaseclass_.leftside).in(roles));

        }


        Predicate premissive = cb.and(
                cb.isFalse(securityLinkRootPremissive.get(SecurityLink_.softDelete)),
                cb.or(cb.equal(securityLinkRootPremissive.get(SecurityLink_.value), op), cb.equal(securityLinkRootPremissive.get(SecurityLink_.value), allOpId)),
                cb.or(cb.equal(securityLinkRootPremissive.get(SecurityLink_.rightside).type(), Clazz.class), cb.equal(securityLinkRootPremissive.get(SecurityLink_.rightside).type(), ClazzLink.class)),
                cb.equal(securityLinkRootPremissive.get(SecurityLink_.simplevalue), IOperation.Access.allow.name()),
                rolesPredicatePremissive


        );

        subPremissive.select(rightside.get(Clazz_.id)).where(premissive);
        return subPremissive;

    }

    private Subquery<String> getPremissiveSubQueryForTenantAndUser(CommonAbstractCriteria query, CriteriaBuilder cb, User user, List<Tenant> tenants, Operation op) {

        Subquery<String> subPremissive = query.subquery(String.class);
        Root<SecurityLink> securityLinkRootPremissive = subPremissive.from(SecurityLink.class);
        Join<SecurityLink, Clazz> rightside = cb.treat(securityLinkRootPremissive.join(SecurityLink_.rightside), Clazz.class);
        Root<UserToBaseClass> userToBaseClassRoot = cb.treat(securityLinkRootPremissive, UserToBaseClass.class);
        Root<TenantToBaseClassPremission> tenantToBaseClassPremissionRoot = cb.treat(securityLinkRootPremissive, TenantToBaseClassPremission.class);
        Operation allOpId = getAllOperation();


        Predicate userPredicatePremissive = cb.and(
                cb.equal(userToBaseClassRoot.get(UserToBaseClass_.leftside), user));
        Predicate tenantPredicatePremissive = cb.and(
                tenantToBaseClassPremissionRoot.get(TenantToBaseClassPremission_.leftside).in(tenants));
        Predicate premissive = cb.and(
                cb.isFalse(securityLinkRootPremissive.get(SecurityLink_.softDelete)),
                cb.or(cb.equal(securityLinkRootPremissive.get(SecurityLink_.value), op), cb.equal(securityLinkRootPremissive.get(SecurityLink_.value), allOpId)),
                cb.or(cb.equal(securityLinkRootPremissive.get(SecurityLink_.rightside).type(), Clazz.class), cb.equal(securityLinkRootPremissive.get(SecurityLink_.rightside).type(), ClazzLink.class)),
                cb.equal(securityLinkRootPremissive.get(SecurityLink_.simplevalue), IOperation.Access.allow.name()),
                cb.or(
                        userPredicatePremissive,
                        tenantPredicatePremissive

                )


        );

        subPremissive.select(rightside.get(Clazz_.id)).where(premissive);
        return subPremissive;

    }

    private List<SecurityLink> getAllowAllLinks(CriteriaBuilder cb, List<Role> roles, User user, List<Tenant> tenants, Operation op) {
     /*   Clazz tenantToBaseClassClazz = Baseclass.getClazzbyname(TenantToBaseClassPremission.class.getCanonicalName());
        Clazz roleToBaseClassClazz = Baseclass.getClazzbyname(RoleToBaseclass.class.getCanonicalName());
        Clazz userToBaseClassClazz = Baseclass.getClazzbyname(UserToBaseClass.class.getCanonicalName());*/
        CriteriaQuery<SecurityLink> subPremissive = cb.createQuery(SecurityLink.class);
        Root<SecurityLink> securityLinkRootPremissive = subPremissive.from(SecurityLink.class);
        Root<UserToBaseClass> userToBaseClassRoot = cb.treat(securityLinkRootPremissive, UserToBaseClass.class);
        Root<RoleToBaseclass> roleToBaseclassRoot = cb.treat(securityLinkRootPremissive, RoleToBaseclass.class);
        Root<TenantToBaseClassPremission> tenantToBaseClassPremissionRoot = cb.treat(securityLinkRootPremissive, TenantToBaseClassPremission.class);
        Operation allOpId = getAllOperation();

        Predicate rolesPredicatePremissive = cb.or();
        if (!roles.isEmpty()) {
            rolesPredicatePremissive = cb.and(
                    roleToBaseclassRoot.get(RoleToBaseclass_.leftside).in(roles));

        }


        Predicate userPredicatePremissive = cb.and(
                cb.equal(userToBaseClassRoot.get(UserToBaseClass_.leftside), user));
        Predicate tenantPredicatePremissive = cb.or();
        if (!tenants.isEmpty()) {
            tenantPredicatePremissive = cb.and(
                    tenantToBaseClassPremissionRoot.get(TenantToBaseClassPremission_.leftside).in(tenants));
        }

        Clazz securityWildcard = Baseclass.getClazzbyname(SecurityWildcard.class.getCanonicalName());
        Predicate premissive = cb.and(
                cb.or(cb.equal(securityLinkRootPremissive.get(SecurityLink_.value), op), cb.equal(securityLinkRootPremissive.get(SecurityLink_.value), allOpId)),
                cb.equal(securityLinkRootPremissive.get(SecurityLink_.rightside), securityWildcard),
                cb.or(
                        userPredicatePremissive,
                        tenantPredicatePremissive,
                        rolesPredicatePremissive

                ),
                cb.isFalse(securityLinkRootPremissive.get(SecurityLink_.softDelete))


        );

        subPremissive.select(securityLinkRootPremissive).where(premissive);
        TypedQuery<SecurityLink> query = em.createQuery(subPremissive);
        List<SecurityLink> all = query.getResultList();

        return all;

    }


    private <X, E, Z extends X> PluralAttribute<Z, List<E>, E> convertListToPlural(ListAttribute<X, E> attr, Class<Z> other) {
        return (PluralAttribute<Z, List<E>, E>) attr;
    }

    @Override
    public <T> List<T> getAll(Class<T> c) {
        return getAll(c, null, null);
    }


    @Override
    public <T> List<T> getAll(Class<T> c, Integer pageSize, Integer currentPage) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> q = cb.createQuery(c);
        Root<T> r = q.from(c);
        q.select(r);
        TypedQuery<T> query = em.createQuery(q);
        if (pageSize != null && pageSize > 0 && currentPage != null && currentPage > -1) {
            setPageQuery(pageSize, currentPage, query);
        }
        return query.getResultList();

    }

    @Override
    public <T> long countAll(Class<T> c) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<T> r = q.from(c);
        q.select(cb.count(r));
        TypedQuery<Long> query = em.createQuery(q);

        return query.getSingleResult();

    }


    /**
     * @param user user
     * @param op operation
     * @return a list of denied baseclasses  for user using Operation
     */
    @Override
    public Pair<List<Baseclass>, List<Baseclass>> getDenied(User user, Operation op) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserToBaseClass> q = cb.createQuery(UserToBaseClass.class);
        Root<UserToBaseClass> r = q.from(UserToBaseClass.class);

        Predicate p1 = cb.and(
                cb.or(cb.isFalse(r.get(SecurityLink_.softDelete)), r.get(SecurityLink_.softDelete).isNull()),
                cb.equal(r.get(UserToBaseClass_.leftside), user),
                cb.equal(r.get(UserToBaseClass_.value), op),
                cb.equal(r.get(UserToBaseClass_.simplevalue), IOperation.Access.deny.name())
        );

        //p1=cb.or(p1,p2);
        List<Predicate> preds = new ArrayList<>();
        preds.add(p1);
        finalizeQuery(r, q, preds, cb);
        TypedQuery<UserToBaseClass> query = em.createQuery(q);

        List<UserToBaseClass> deniedUsers = query.getResultList();
        CriteriaQuery<RoleToBaseclass> q1 = cb.createQuery(RoleToBaseclass.class);
        Root<RoleToBaseclass> r1 = q1.from(RoleToBaseclass.class);
        Join<RoleToBaseclass, Role> j1 = cb.treat(r1.join(RoleToBaseclass_.leftside, JoinType.LEFT), Role.class);
        Join<Role, RoleToUser> j2 = j1.join(Role_.roleToUser, JoinType.INNER);

        Predicate p2 = cb.and(
                cb.or(cb.isFalse(r1.get(SecurityLink_.softDelete)), r1.get(SecurityLink_.softDelete).isNull()),

                cb.equal(j2.get(RoleToUser_.rightside), user),
                cb.equal(r1.get(RoleToBaseclass_.value), op),
                cb.equal(r1.get(RoleToBaseclass_.simplevalue), IOperation.Access.deny.name())
        );
        List<Predicate> preds1 = new ArrayList<>();
        preds.add(p2);
        finalizeQuery(r1, q1, preds, cb);
        TypedQuery<RoleToBaseclass> query1 = em.createQuery(q1);
        List<RoleToBaseclass> deniedRoles = query1.getResultList();
        List<Baseclass> deniedUsersBase = new ArrayList<>();
        List<Baseclass> deniedRolesBase = new ArrayList<>();
        for (RoleToBaseclass roleToBaseclass : deniedRoles) {
            deniedRolesBase.add(roleToBaseclass.getRightside());
        }
        for (UserToBaseClass userToBaseClass : deniedUsers) {
            deniedUsersBase.add(userToBaseClass.getRightside());
        }

        return new MutablePair<>(deniedUsersBase, deniedRolesBase);
    }


    private List<Role> getAllRoles(User user) {
        Class<RoleToUser> c = RoleToUser.class;
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<RoleToUser> q = cb.createQuery(c);
        Root<RoleToUser> r = q.from(c);
        List<Predicate> preds = new ArrayList<>();
        preds.add(cb.and(cb.equal(r.get(RoleToUser_.rightside), user),cb.isFalse(r.get(RoleToUser_.softDelete))));
        finalizeQuery(r, q, preds, cb);
        TypedQuery<RoleToUser> query = em.createQuery(q);
        List<RoleToUser> links = getResultList(query);
        List<Role> roles = new ArrayList<>();
        for (RoleToUser link : links) {
            roles.add(link.getLeftside());
        }


        return roles;
    }

    @Override
    public <T extends Baseclass> void addTenantsPredicate(List<Predicate> existingPredicates, From<?, T> r,
                                                          CriteriaBuilder cb, Set<String> tenantsIds) {
        Join<T, Tenant> tenantJoin = r.join(Baseclass_.tenant);
        existingPredicates.add(tenantJoin.get(Tenant_.id).in(tenantsIds));


    }

    @Override
    public <T extends Baseclass> void addClazzPredicate(List<Predicate> existingPredicates, From<?, T> r,
                                                        CriteriaBuilder cb, Set<String> clazzIds) {
        Join<T, Clazz> tenantJoin = r.join(Baseclass_.clazz);
        existingPredicates.add(tenantJoin.get(Clazz_.id).in(clazzIds));


    }

    /**
     * Add the Predicates for list of Categories, we will want only instances
     * linked with one of the categories.
     *
     * @param existingPredicates predicates
     * @param r root
     * @param cb criteria builder
     * @param categories categories
     */
    @Override
    public <T extends Baseclass> void addCategoriesPredicate(List<Predicate> existingPredicates, From<?, T> r,
                                                             CriteriaBuilder cb, List<CategoryIdFiltering> categories) {
        Join<CategoryToBaseClass, Baseclass> categoryToBaseClassJoin = null;
        if (categories != null && !categories.isEmpty()) {
            Join<T, CategoryToBaseClass> baseclassCategoryJoin = r.join(Baseclass_.categories);
            categoryToBaseClassJoin = baseclassCategoryJoin.join(CategoryToBaseClass_.rightside);
        }

        List<Predicate> categoriesPredicates = new ArrayList<>();

        if (categories != null) {
            for (CategoryIdFiltering category : categories) {
                categoriesPredicates.add(cb.equal(categoryToBaseClassJoin.get(Baseclass_.id), category.getId()));
            }
        }

        Predicate catPred = null;
        Predicate[] pr;
        if (!categoriesPredicates.isEmpty()) {

            pr = new Predicate[categoriesPredicates.size()];
            pr = categoriesPredicates.toArray(pr);
            catPred = cb.or(pr);
        }

        if (existingPredicates == null) {
            existingPredicates = new ArrayList<>();
        }
        if (catPred != null) {
            existingPredicates.add(catPred);
        }
    }


    @Override
    public void addTenantToBaseClass(Baseclass b, Tenant tenant, SecurityContext securityContext) {
        TenantToBaseClassPremission tenantToBaseClassPremission = new TenantToBaseClassPremission(b.getName(), securityContext);
        tenantToBaseClassPremission.setTenant(tenant);
        tenantToBaseClassPremission.setBaseclass(b);

    }

    @Override
    public void refresh(Object o) {
        em.refresh(o);
    }

    @Override
    public EntityManager getEm() {
        return em;
    }

    @Override
    public void setEm(EntityManager em) {
        this.em = em;
    }

    @Override
    public <T extends Baseclass> T createBaseclass(BaseclassCreationContainer container,
                                                   SecurityContext securityContext) {
        T created = null;
        try {
            String className = container.getClazzName();

            created = Baseclass.newInstance(className, container.getNameOfInstance(), securityContext);
            Class<?> c = created.getClass();
            List<FieldSetContainer<?>> fields = container.getFields();
            for (FieldSetContainer<?> field : fields) {
                try {
                    c.getField(field.getName()).set(created, field.getValue());
                } catch (NoSuchFieldException e) {
                    logger.log(Level.WARNING, "field " + field.getName() + " doesn't exists", e);
                }

            }

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SecurityException e) {
            logger.log(Level.SEVERE, "unable to create instance of: " + container.getClazzName(), e);
        }
        return created;
    }

    @Override
    public void refrehEntityManager() {
        em.flush();
        em.clear();
    }


    @Override
    @Transactional
    public void massMerge(List<?> toMerge,boolean updatedate) {
        List<Object> events=new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();
        for (Object o : toMerge) {
            if(o instanceof Baseclass){
                Baseclass baseclass = (Baseclass) o;
                boolean created=baseclass.getUpdateDate()==null;
                if(updatedate){
                    baseclass.setUpdateDate(now);
                }
                if(logger.isLoggable(Level.FINE)){
                    logger.fine("merging "+ baseclass.getId() +" updateDate flag is "+updatedate +" update date is "+baseclass.getUpdateDate());
                }
                updateSearchKey(baseclass);
                if(created){
                    BaseclassCreated<?> baseclassCreated=new BaseclassCreated<>(baseclass);
                    events.add(baseclassCreated);
                }
                else{
                    BaseclassUpdated<?> baseclassUpdated=new BaseclassUpdated<>(baseclass);
                    events.add(baseclassUpdated);
                }

            }

            em.merge(o);
        }
        for (Object event : events) {
            eventPublisher.publishEvent(event);
        }
    }

    public void updateSearchKey(Baseclass b){
        try {
            if (isFreeTextSupport(b.getClass())) {
                String freeText= Stream.of( Introspector.getBeanInfo(b.getClass(), Object.class).getPropertyDescriptors()).filter(this::isPropertyForTextSearch).map(PropertyDescriptor::getReadMethod).filter(this::isIncludeMethod).map(f-> invoke(b, f)).filter(Objects::nonNull).map(f->f+"").filter(f->!f.isEmpty()).collect(Collectors.joining("|"));
                b.setSearchKey(freeText);
                logger.fine("Free Text field for "+b.getId() +" is set");

            }
        }

        catch (Exception e){
            logger.log(Level.SEVERE,"unable to set free text field",e);
        }
    }
    private static final Map<String, Boolean> freeTextSuportMap = new ConcurrentHashMap<>();

    private Object invoke(Baseclass b, Method f) {
        try {
            return f.invoke(b);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.log(Level.SEVERE,"unable to invoke method",e);
        }
        return null;
    }

    private boolean isFreeTextSupport(Class<? extends Baseclass> aClass) {
        return freeTextSuportMap.computeIfAbsent(aClass.getCanonicalName(),f-> checkFreeTextSupport(aClass));
    }
    private boolean isIncludeMethod(Method f) {
        if(f==null||f.isAnnotationPresent(Transient.class)){
            return false;
        }
        FullTextSearchOptions fullTextSearchOptions=f.getAnnotation(FullTextSearchOptions.class);

        return fullTextSearchOptions==null||fullTextSearchOptions.include();
    }

    private boolean checkFreeTextSupport(Class<? extends Baseclass> aClass) {
        FullTextSearch annotation = aClass.getAnnotation(FullTextSearch.class);
        return annotation!=null&&annotation.supported();
    }

    private boolean isPropertyForTextSearch(PropertyDescriptor f) {
        return propertyTypes.contains(f.getPropertyType());
    }

    private static final Set<Class<?>> propertyTypes= Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            String.class,
            int.class,
            double.class,
            float.class,
            int.class,
            long.class,
            short.class,
            Double.class,
            Float.class,
            Integer.class,
            Long.class,
            Short.class

    )));

    @Override
    @Transactional
    public <T extends Baseclass> List<T> findByIds(Class<T> c, Set<String> requested) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> q = cb.createQuery(c);
        Root<T> r = q.from(c);
        q.select(r).where(r.get(Baseclass_.id).in(requested));
        TypedQuery<T> query = em.createQuery(q);
        return query.getResultList();
    }

    @Override
    @Transactional
    public void massDelete(MassDeleteRequest massDeleteRequest) {
        Set<String> ids = massDeleteRequest.getBaseclass().parallelStream().map(f -> f.getId()).collect(Collectors.toSet());
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete<Baseclass> q = cb.createCriteriaDelete(Baseclass.class);
        Root<Baseclass> r = q.from(Baseclass.class);
        q.where(r.get(Baseclass_.id).in(ids));
        em.createQuery(q).executeUpdate();
    }

    @Override
    public List<BaseclassCount> getBaseclassCount(BaseclassCountRequest baseclassCountRequest, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<BaseclassCount> q = cb.createQuery(BaseclassCount.class);
        Root<Baseclass> r = q.from(Baseclass.class);
        Join<Baseclass,Clazz> join=r.join(Baseclass_.clazz);

        List<Predicate> preds=new ArrayList<>();
        preds.add(r.get(Baseclass_.tenant).isNotNull());
        preds.add(cb.isFalse(r.get(Baseclass_.softDelete)));
        QueryInformationHolder<Baseclass> queryInformationHolder=new QueryInformationHolder<>(baseclassCountRequest,Baseclass.class,securityContext);
        prepareQuery(queryInformationHolder,preds,cb,q,r);
        Predicate[] predsArr=new Predicate[preds.size()];
        preds.toArray(predsArr);
        CriteriaQuery<BaseclassCount> select;
        List<Expression<?>> groupBy;
        if(baseclassCountRequest.isGroupByTenant()){
             select = q.select(cb.construct(BaseclassCount.class,r.get(Baseclass_.tenant), join.get(Clazz_.name), cb.count(r.get(Baseclass_.id))));
            groupBy= Arrays.asList(r.get(Baseclass_.tenant),join.get(Clazz_.name));
        }
        else{
            select = q.select(cb.construct(BaseclassCount.class, join.get(Clazz_.name), cb.count(r.get(Baseclass_.id))));
            groupBy= Arrays.asList(join.get(Clazz_.name));


        }
        select.where(predsArr)
                .groupBy(groupBy)
                .orderBy(cb.asc(join.get(Clazz_.name)));
        return em.createQuery(q).getResultList();
    }

    @Override
    @Transactional
    public void merge(Object base) {
        merge(base,true);
    }

    @Override
    @Transactional
    public void massMerge(List<?> toMerge) {
        massMerge(toMerge,true);
    }
}
