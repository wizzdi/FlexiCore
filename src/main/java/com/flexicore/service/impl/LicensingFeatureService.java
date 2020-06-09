package com.flexicore.service.impl;


import com.flexicore.data.LicensingFeatureRepository;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.interfaces.FlexiCoreService;
import com.flexicore.model.Baseclass;
import com.flexicore.model.licensing.LicensingFeature;
import com.flexicore.model.licensing.LicensingProduct;
import com.flexicore.request.LicensingFeatureCreate;
import com.flexicore.request.LicensingFeatureFiltering;
import com.flexicore.request.LicensingFeatureUpdate;
import com.flexicore.security.SecurityContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Autowired;
import javax.ws.rs.BadRequestException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@Primary
@Component
public class LicensingFeatureService implements FlexiCoreService {


    @Autowired
    private LicensingFeatureRepository repository;

    @Autowired
    private LicensingEntityService licensingEntityService;

   private Logger logger = Logger.getLogger(getClass().getCanonicalName());


    public <T extends Baseclass> T getByIdOrNull(String id, Class<T> c, List<String> batchString, SecurityContext securityContext) {
        return repository.getByIdOrNull(id, c, batchString, securityContext);
    }


    public <T extends Baseclass> List<T> listByIds(Class<T> c, Set<String> ids, SecurityContext securityContext) {
        return repository.listByIds(c, ids, securityContext);
    }

    public LicensingFeature createLicensingFeature(LicensingFeatureCreate pluginCreationContainer, SecurityContext securityContext) {
        LicensingFeature licensingFeature = createLicensingFeatureNoMerge(pluginCreationContainer, securityContext);
        repository.merge(licensingFeature);
        return licensingFeature;


    }

    public LicensingFeature createLicensingFeatureNoMerge(LicensingFeatureCreate licensingFeatureCreate, SecurityContext securityContext) {
        LicensingFeature licensingFeature = new LicensingFeature(licensingFeatureCreate.getName(), securityContext);
        updateLicensingFeatureNoMerge(licensingFeature, licensingFeatureCreate);
        return licensingFeature;
    }

    private boolean updateLicensingFeatureNoMerge(LicensingFeature licensingFeature, LicensingFeatureCreate licensingFeatureCreate) {
        boolean update = licensingEntityService.updateLicensingEntityNoMerge(licensingFeature, licensingFeatureCreate);
        if(licensingFeatureCreate.getLicensingProduct()!=null && (licensingFeature.getProduct()==null || !licensingFeatureCreate.getLicensingProduct().getId().equals(licensingFeature.getProduct().getId()))){
            licensingFeature.setProduct(licensingFeatureCreate.getLicensingProduct());
            update=true;
        }
        return update;
    }


    public LicensingFeature updateLicensingFeature(LicensingFeatureUpdate licensingFeatureUpdate, SecurityContext securityContext) {
        LicensingFeature licensingFeature = licensingFeatureUpdate.getLicensingFeature();
        if (updateLicensingFeatureNoMerge(licensingFeature, licensingFeatureUpdate)) {
            repository.merge(licensingFeature);
        }
        return licensingFeature;
    }

    public List<LicensingFeature> listAllLicensingFeatures(LicensingFeatureFiltering licensingFeatureFiltering, SecurityContext securityContext) {
        return repository.listAllLicensingFeatures(licensingFeatureFiltering, securityContext);
    }

    public void validate(LicensingFeatureCreate licensingFeatureCreate, SecurityContext securityContext) {
        licensingEntityService.validate(licensingFeatureCreate, securityContext);

    }

    public void validate(LicensingFeatureFiltering licensingFeatureFiltering, SecurityContext securityContext) {
        licensingEntityService.validate(licensingFeatureFiltering,securityContext);
        Set<String> productIds=licensingFeatureFiltering.getLicensingProductsIds();
        Map<String, LicensingProduct> productMap=productIds.isEmpty()?new HashMap<>():listByIds(LicensingProduct.class,productIds,securityContext).parallelStream().collect(Collectors.toMap(f->f.getId(),f->f));
        productIds.removeAll(productMap.keySet());
        if(!productIds.isEmpty()){
            throw new BadRequestException("No LicensingProduct with ids "+productIds);
        }
        licensingFeatureFiltering.setLicensingProducts(new ArrayList<>(productMap.values()));
    }

    public PaginationResponse<LicensingFeature> getAllLicensingFeatures(LicensingFeatureFiltering licensingFeatureFiltering, SecurityContext securityContext) {
        List<LicensingFeature> list = listAllLicensingFeatures(licensingFeatureFiltering, securityContext);
        long count = repository.countAllLicensingFeatures(licensingFeatureFiltering, securityContext);
        return new PaginationResponse<>(list, licensingFeatureFiltering, count);
    }


}