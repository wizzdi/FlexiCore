package com.flexicore.data;

import com.flexicore.annotations.InheritedComponent;
import com.flexicore.data.impl.BaseclassRepository;
import com.flexicore.model.QueryInformationHolder;
import com.flexicore.model.licensing.LicenseRequestToEntity;
import com.flexicore.model.licensing.LicenseRequestToProduct;
import com.flexicore.request.LicenseRequestToProductFiltering;
import com.flexicore.security.SecurityContext;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@InheritedComponent
public class LicenseRequestToProductRepository extends BaseclassRepository {


    public List<LicenseRequestToProduct> listAllLicenseRequestToProducts(LicenseRequestToProductFiltering licenseRequestToProductFiltering, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<LicenseRequestToProduct> q = cb.createQuery(LicenseRequestToProduct.class);
        Root<LicenseRequestToProduct> r = q.from(LicenseRequestToProduct.class);
        List<Predicate> preds = new ArrayList<>();
        addLicenseRequestToProductsPredicates(licenseRequestToProductFiltering,r,cb,preds);
        QueryInformationHolder<LicenseRequestToProduct> queryInformationHolder = new QueryInformationHolder<>(licenseRequestToProductFiltering,LicenseRequestToProduct.class, securityContext);
        return getAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    private void addLicenseRequestToProductsPredicates(LicenseRequestToProductFiltering licenseRequestToProductFiltering, Root<LicenseRequestToProduct> r,  CriteriaBuilder cb, List<Predicate> preds) {
        LicenseRequestToEntityRepository.addLicenseRequestToEntitiesPredicates(licenseRequestToProductFiltering,r,cb,preds);
    }

    public long countAllLicenseRequestToProducts(LicenseRequestToProductFiltering licenseRequestToProductFiltering, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<LicenseRequestToProduct> r = q.from(LicenseRequestToProduct.class);
        List<Predicate> preds = new ArrayList<>();
        addLicenseRequestToProductsPredicates(licenseRequestToProductFiltering,r,cb,preds);
        QueryInformationHolder<LicenseRequestToProduct> queryInformationHolder = new QueryInformationHolder<>(licenseRequestToProductFiltering,LicenseRequestToProduct.class, securityContext);
        return countAllFiltered(queryInformationHolder, preds, cb, q, r);
    }
}