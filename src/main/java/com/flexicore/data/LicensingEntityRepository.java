package com.flexicore.data;

import com.flexicore.annotations.InheritedComponent;
import com.flexicore.data.impl.BaseclassRepository;
import com.flexicore.model.QueryInformationHolder;
import com.flexicore.model.licensing.LicensingEntity;
import com.flexicore.model.licensing.LicensingEntity_;
import com.flexicore.request.LicensingEntityFiltering;
import com.flexicore.security.SecurityContext;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@InheritedComponent
public class LicensingEntityRepository extends BaseclassRepository {


    public List<LicensingEntity> listAllLicensingEntities(LicensingEntityFiltering licensingEntityFiltering, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<LicensingEntity> q = cb.createQuery(LicensingEntity.class);
        Root<LicensingEntity> r = q.from(LicensingEntity.class);
        List<Predicate> preds = new ArrayList<>();
        addLicensingEntitiesPredicates(licensingEntityFiltering,r,cb,preds);
        QueryInformationHolder<LicensingEntity> queryInformationHolder = new QueryInformationHolder<>(licensingEntityFiltering,LicensingEntity.class, securityContext);
        return getAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    public static <T extends LicensingEntity> void addLicensingEntitiesPredicates(LicensingEntityFiltering licensingEntityFiltering, Root<T> r,  CriteriaBuilder cb, List<Predicate> preds) {
        if(licensingEntityFiltering.getCanonicalNames()!=null &&!licensingEntityFiltering.getCanonicalNames().isEmpty()){
            preds.add(r.get(LicensingEntity_.canonicalName).in(licensingEntityFiltering.getCanonicalNames()));
        }
    }

    public long countAllLicensingEntities(LicensingEntityFiltering licensingEntityFiltering, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<LicensingEntity> r = q.from(LicensingEntity.class);
        List<Predicate> preds = new ArrayList<>();
        addLicensingEntitiesPredicates(licensingEntityFiltering,r,cb,preds);
        QueryInformationHolder<LicensingEntity> queryInformationHolder = new QueryInformationHolder<>(licensingEntityFiltering,LicensingEntity.class, securityContext);
        return countAllFiltered(queryInformationHolder, preds, cb, q, r);
    }
}