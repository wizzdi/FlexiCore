package com.flexicore.data;

import com.flexicore.annotations.InheritedComponent;
import com.flexicore.data.impl.BaseclassRepository;
import com.flexicore.model.QueryInformationHolder;
import com.flexicore.model.licensing.*;
import com.flexicore.request.LicenseRequestToEntityFiltering;
import com.flexicore.security.SecurityContext;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@InheritedComponent
public class LicenseRequestToEntityRepository extends BaseclassRepository {


    public List<LicenseRequestToEntity> listAllLicenseRequestToEntities(LicenseRequestToEntityFiltering licenseRequestToEntityFiltering, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<LicenseRequestToEntity> q = cb.createQuery(LicenseRequestToEntity.class);
        Root<LicenseRequestToEntity> r = q.from(LicenseRequestToEntity.class);
        List<Predicate> preds = new ArrayList<>();
        addLicenseRequestToEntitiesPredicates(licenseRequestToEntityFiltering,r,cb,preds);
        QueryInformationHolder<LicenseRequestToEntity> queryInformationHolder = new QueryInformationHolder<>(licenseRequestToEntityFiltering,LicenseRequestToEntity.class, securityContext);
        return getAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    public static <T extends LicenseRequestToEntity> void addLicenseRequestToEntitiesPredicates(LicenseRequestToEntityFiltering licenseRequestToEntityFiltering, Root<T> r,  CriteriaBuilder cb, List<Predicate> preds) {
        if(licenseRequestToEntityFiltering.getLicenseRequests()!=null && !licenseRequestToEntityFiltering.getLicenseRequests().isEmpty()){
            Set<String> ids=licenseRequestToEntityFiltering.getLicenseRequests().parallelStream().map(f->f.getId()).collect(Collectors.toSet());
            Join<T, LicenseRequest> join=r.join(LicenseRequestToEntity_.licenseRequest);
            preds.add(join.get(LicenseRequest_.id).in(ids));
        }
    }

    public long countAllLicenseRequestToEntities(LicenseRequestToEntityFiltering licenseRequestToEntityFiltering, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<LicenseRequestToEntity> r = q.from(LicenseRequestToEntity.class);
        List<Predicate> preds = new ArrayList<>();
        addLicenseRequestToEntitiesPredicates(licenseRequestToEntityFiltering,r,cb,preds);
        QueryInformationHolder<LicenseRequestToEntity> queryInformationHolder = new QueryInformationHolder<>(licenseRequestToEntityFiltering,LicenseRequestToEntity.class, securityContext);
        return countAllFiltered(queryInformationHolder, preds, cb, q, r);
    }
}