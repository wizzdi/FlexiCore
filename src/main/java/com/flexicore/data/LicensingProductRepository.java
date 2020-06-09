package com.flexicore.data;

import com.flexicore.annotations.InheritedComponent;
import com.flexicore.data.impl.BaseclassRepository;
import com.flexicore.model.QueryInformationHolder;
import com.flexicore.model.licensing.LicensingProduct;
import com.flexicore.request.LicensingProductFiltering;
import com.flexicore.security.SecurityContext;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@InheritedComponent
public class LicensingProductRepository extends BaseclassRepository {


    public List<LicensingProduct> listAllLicensingProducts(LicensingProductFiltering licensingProductFiltering, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<LicensingProduct> q = cb.createQuery(LicensingProduct.class);
        Root<LicensingProduct> r = q.from(LicensingProduct.class);
        List<Predicate> preds = new ArrayList<>();
        addLicensingProductsPredicates(licensingProductFiltering,r,cb,preds);
        QueryInformationHolder<LicensingProduct> queryInformationHolder = new QueryInformationHolder<>(licensingProductFiltering,LicensingProduct.class, securityContext);
        return getAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    private void addLicensingProductsPredicates(LicensingProductFiltering licensingProductFiltering, Root<LicensingProduct> r, CriteriaBuilder cb, List<Predicate> preds) {
        LicensingEntityRepository.addLicensingEntitiesPredicates(licensingProductFiltering,r,cb,preds);
    }

    public long countAllLicensingProducts(LicensingProductFiltering licensingProductFiltering, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<LicensingProduct> r = q.from(LicensingProduct.class);
        List<Predicate> preds = new ArrayList<>();
        addLicensingProductsPredicates(licensingProductFiltering,r,cb,preds);
        QueryInformationHolder<LicensingProduct> queryInformationHolder = new QueryInformationHolder<>(licensingProductFiltering,LicensingProduct.class, securityContext);
        return countAllFiltered(queryInformationHolder, preds, cb, q, r);
    }
}