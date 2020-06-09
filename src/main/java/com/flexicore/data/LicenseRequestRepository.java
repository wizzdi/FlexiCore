package com.flexicore.data;

import com.flexicore.annotations.InheritedComponent;
import com.flexicore.data.impl.BaseclassRepository;
import com.flexicore.model.QueryInformationHolder;
import com.flexicore.model.licensing.*;
import com.flexicore.request.LicenseRequestFiltering;
import com.flexicore.security.SecurityContext;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@InheritedComponent
public class LicenseRequestRepository extends BaseclassRepository {


    public List<LicenseRequest> listAllLicenseRequests(LicenseRequestFiltering licenseRequestFiltering, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<LicenseRequest> q = cb.createQuery(LicenseRequest.class);
        Root<LicenseRequest> r = q.from(LicenseRequest.class);
        List<Predicate> preds = new ArrayList<>();
        addLicenseRequestsPredicates(licenseRequestFiltering,r,cb,preds);
        QueryInformationHolder<LicenseRequest> queryInformationHolder = new QueryInformationHolder<>(licenseRequestFiltering,LicenseRequest.class, securityContext);
        return getAllFiltered(queryInformationHolder, preds, cb, q, r);
    }

    private void addLicenseRequestsPredicates(LicenseRequestFiltering licenseRequestFiltering, Root<LicenseRequest> r, CriteriaBuilder cb, List<Predicate> preds) {
        Join<LicenseRequest, LicenseRequestToEntity> join=null;
        if(licenseRequestFiltering.getLicensingFeatures()!=null && !licenseRequestFiltering.getLicensingFeatures().isEmpty()){
            Set<String> ids=licenseRequestFiltering.getLicensingFeatures().parallelStream().map(f->f.getId()).collect(Collectors.toSet());
             join=join==null?r.join(LicenseRequest_.requestToEntity):join;
            Join<LicenseRequestToEntity, LicensingFeature> join1=cb.treat(join.join(LicenseRequestToEntity_.licensingEntity),LicensingFeature.class);
            preds.add(join1.get(LicensingFeature_.id).in(ids));
        }


        if(licenseRequestFiltering.getLicensingProducts()!=null && !licenseRequestFiltering.getLicensingProducts().isEmpty()){
            Set<String> ids=licenseRequestFiltering.getLicensingProducts().parallelStream().map(f->f.getId()).collect(Collectors.toSet());
             join=join==null?r.join(LicenseRequest_.requestToEntity):join;
            Join<LicenseRequestToEntity, LicensingProduct> join1=cb.treat(join.join(LicenseRequestToEntity_.licensingEntity),LicensingProduct.class);
            preds.add(join1.get(LicensingProduct_.id).in(ids));
        }
        if(licenseRequestFiltering.getSigned()!=null){
            preds.add(licenseRequestFiltering.getSigned()?r.get(LicenseRequest_.license).isNotNull():r.get(LicenseRequest_.license).isNull());
        }
        if(licenseRequestFiltering.getExpirationDateAfter()!=null){
            join=join==null?r.join(LicenseRequest_.requestToEntity):join;
            preds.add(cb.greaterThan(join.get(LicenseRequestToEntity_.expiration),licenseRequestFiltering.getExpirationDateAfter()));
        }

    }

    public long countAllLicenseRequests(LicenseRequestFiltering licenseRequestFiltering, SecurityContext securityContext) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<LicenseRequest> r = q.from(LicenseRequest.class);
        List<Predicate> preds = new ArrayList<>();
        addLicenseRequestsPredicates(licenseRequestFiltering,r,cb,preds);
        QueryInformationHolder<LicenseRequest> queryInformationHolder = new QueryInformationHolder<>(licenseRequestFiltering,LicenseRequest.class, securityContext);
        return countAllFiltered(queryInformationHolder, preds, cb, q, r);
    }
}