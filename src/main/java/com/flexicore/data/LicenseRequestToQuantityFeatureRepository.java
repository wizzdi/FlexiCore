package com.flexicore.data;

import com.flexicore.annotations.InheritedComponent;
import com.flexicore.data.impl.BaseclassRepository;
import com.flexicore.model.QueryInformationHolder;
import com.flexicore.model.licensing.LicenseRequestToQuantityFeature;
import com.flexicore.request.LicenseRequestToQuantityFeatureFiltering;
import com.flexicore.security.SecurityContext;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@InheritedComponent
public class LicenseRequestToQuantityFeatureRepository extends BaseclassRepository {


    public List<LicenseRequestToQuantityFeature> listAllLicenseRequestToQuantityFeatures(LicenseRequestToQuantityFeatureFiltering licenseRequestToQuantityFeatureFiltering, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<LicenseRequestToQuantityFeature> q = cb.createQuery(LicenseRequestToQuantityFeature.class);
        Root<LicenseRequestToQuantityFeature> r = q.from(LicenseRequestToQuantityFeature.class);
        List<Predicate> preds = new ArrayList<>();
        addLicenseRequestToQuantityFeaturesPredicates(licenseRequestToQuantityFeatureFiltering,r,cb,preds);
        QueryInformationHolder<LicenseRequestToQuantityFeature> queryInformationHolder = new QueryInformationHolder<>(licenseRequestToQuantityFeatureFiltering,LicenseRequestToQuantityFeature.class, securityContext);
        return getAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    private void addLicenseRequestToQuantityFeaturesPredicates(LicenseRequestToQuantityFeatureFiltering licenseRequestToQuantityFeatureFiltering, Root<LicenseRequestToQuantityFeature> r,  CriteriaBuilder cb, List<Predicate> preds) {
        LicenseRequestToFeatureRepository.addLicenseRequestToFeaturesPredicates(licenseRequestToQuantityFeatureFiltering,r,cb,preds);
    }

    public long countAllLicenseRequestToQuantityFeatures(LicenseRequestToQuantityFeatureFiltering licenseRequestToQuantityFeatureFiltering, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<LicenseRequestToQuantityFeature> r = q.from(LicenseRequestToQuantityFeature.class);
        List<Predicate> preds = new ArrayList<>();
        addLicenseRequestToQuantityFeaturesPredicates(licenseRequestToQuantityFeatureFiltering,r,cb,preds);
        QueryInformationHolder<LicenseRequestToQuantityFeature> queryInformationHolder = new QueryInformationHolder<>(licenseRequestToQuantityFeatureFiltering,LicenseRequestToQuantityFeature.class, securityContext);
        return countAllFiltered(queryInformationHolder, preds, cb, q, r);
    }
}