package com.flexicore.data;

import com.flexicore.annotations.InheritedComponent;
import com.flexicore.data.impl.BaseclassRepository;
import com.flexicore.model.QueryInformationHolder;
import com.flexicore.model.licensing.LicenseRequestToFeature;
import com.flexicore.request.LicenseRequestToFeatureFiltering;
import com.flexicore.security.SecurityContext;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@InheritedComponent
public class LicenseRequestToFeatureRepository extends BaseclassRepository {


    public List<LicenseRequestToFeature> listAllLicenseRequestToFeatures(LicenseRequestToFeatureFiltering licenseRequestToFeatureFiltering, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<LicenseRequestToFeature> q = cb.createQuery(LicenseRequestToFeature.class);
        Root<LicenseRequestToFeature> r = q.from(LicenseRequestToFeature.class);
        List<Predicate> preds = new ArrayList<>();
        addLicenseRequestToFeaturesPredicates(licenseRequestToFeatureFiltering,r,cb,preds);
        QueryInformationHolder<LicenseRequestToFeature> queryInformationHolder = new QueryInformationHolder<>(licenseRequestToFeatureFiltering,LicenseRequestToFeature.class, securityContext);
        return getAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    public static <T extends LicenseRequestToFeature> void addLicenseRequestToFeaturesPredicates(LicenseRequestToFeatureFiltering licenseRequestToFeatureFiltering, Root<T> r,  CriteriaBuilder cb, List<Predicate> preds) {
        LicenseRequestToEntityRepository.addLicenseRequestToEntitiesPredicates(licenseRequestToFeatureFiltering,r,cb,preds);
    }

    public long countAllLicenseRequestToFeatures(LicenseRequestToFeatureFiltering licenseRequestToFeatureFiltering, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<LicenseRequestToFeature> r = q.from(LicenseRequestToFeature.class);
        List<Predicate> preds = new ArrayList<>();
        addLicenseRequestToFeaturesPredicates(licenseRequestToFeatureFiltering,r,cb,preds);
        QueryInformationHolder<LicenseRequestToFeature> queryInformationHolder = new QueryInformationHolder<>(licenseRequestToFeatureFiltering,LicenseRequestToFeature.class, securityContext);
        return countAllFiltered(queryInformationHolder, preds, cb, q, r);
    }
}