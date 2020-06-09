package com.flexicore.service.impl;


import com.atomikos.publish.EventPublisher;
import com.flexicore.data.LicenseRequestToQuantityFeatureRepository;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.interfaces.FlexiCoreService;
import com.flexicore.model.Baseclass;
import com.flexicore.model.licensing.LicenseRequestToQuantityFeature;
import com.flexicore.model.licensing.LicensingFeature;
import com.flexicore.request.LicenseRequestToQuantityFeatureCreate;
import com.flexicore.request.LicenseRequestToQuantityFeatureFiltering;
import com.flexicore.request.LicenseRequestToQuantityFeatureUpdate;
import com.flexicore.security.SecurityContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Autowired;
import javax.ws.rs.BadRequestException;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;


@Primary
@Component
public class LicenseRequestToQuantityFeatureService implements FlexiCoreService {


    @Autowired
    private LicenseRequestToQuantityFeatureRepository repository;

    @Autowired
    private LicenseRequestToFeatureService licenseRequestToFeatureService;

   private Logger logger = Logger.getLogger(getClass().getCanonicalName());


    public <T extends Baseclass> T getByIdOrNull(String id, Class<T> c, List<String> batchString, SecurityContext securityContext) {
        return repository.getByIdOrNull(id, c, batchString, securityContext);
    }


    public <T extends Baseclass> List<T> listByIds(Class<T> c, Set<String> ids, SecurityContext securityContext) {
        return repository.listByIds(c, ids, securityContext);
    }

    @Autowired
    private ApplicationEventPublisher licenseRequestUpdateEventEvent;

    public LicenseRequestToQuantityFeature createLicenseRequestToQuantityFeature(LicenseRequestToQuantityFeatureCreate pluginCreationContainer, SecurityContext securityContext) {
        LicenseRequestToQuantityFeature licenseRequestToQuantityFeature = createLicenseRequestToQuantityFeatureNoMerge(pluginCreationContainer, securityContext);
        repository.merge(licenseRequestToQuantityFeature);
        licenseRequestUpdateEventEvent.publishEvent(new LicenseRequestUpdateEvent().setLicenseRequest(licenseRequestToQuantityFeature.getLicenseRequest()).setSecurityContext(securityContext));
        return licenseRequestToQuantityFeature;


    }

    public LicenseRequestToQuantityFeature createLicenseRequestToQuantityFeatureNoMerge(LicenseRequestToQuantityFeatureCreate licenseRequestToQuantityFeatureCreate, SecurityContext securityContext) {
        LicenseRequestToQuantityFeature licenseRequestToQuantityFeature = new LicenseRequestToQuantityFeature(licenseRequestToQuantityFeatureCreate.getName(), securityContext);
        updateLicenseRequestToQuantityFeatureNoMerge(licenseRequestToQuantityFeature, licenseRequestToQuantityFeatureCreate);
        return licenseRequestToQuantityFeature;
    }

    private boolean updateLicenseRequestToQuantityFeatureNoMerge(LicenseRequestToQuantityFeature licenseRequestToQuantityFeature, LicenseRequestToQuantityFeatureCreate licenseRequestToQuantityFeatureCreate) {
        boolean update = licenseRequestToFeatureService.updateLicenseRequestToFeatureNoMerge(licenseRequestToQuantityFeature, licenseRequestToQuantityFeatureCreate);
        if (licenseRequestToQuantityFeatureCreate.getQuantityLimit() != null && !licenseRequestToQuantityFeatureCreate.getQuantityLimit().equals(licenseRequestToQuantityFeature.getQuantityLimit())) {
            licenseRequestToQuantityFeature.setQuantityLimit(licenseRequestToQuantityFeatureCreate.getQuantityLimit());
            update = true;
        }

        return update;
    }


    public LicenseRequestToQuantityFeature updateLicenseRequestToQuantityFeature(LicenseRequestToQuantityFeatureUpdate licenseRequestToQuantityFeatureUpdate, SecurityContext securityContext) {
        LicenseRequestToQuantityFeature licenseRequestToQuantityFeature = licenseRequestToQuantityFeatureUpdate.getLicenseRequestToQuantityFeature();
        if (updateLicenseRequestToQuantityFeatureNoMerge(licenseRequestToQuantityFeature, licenseRequestToQuantityFeatureUpdate)) {
            repository.merge(licenseRequestToQuantityFeature);
            licenseRequestUpdateEventEvent.publishEvent(new LicenseRequestUpdateEvent().setLicenseRequest(licenseRequestToQuantityFeature.getLicenseRequest()).setSecurityContext(securityContext));

        }
        return licenseRequestToQuantityFeature;
    }

    public List<LicenseRequestToQuantityFeature> listAllLicenseRequestToQuantityFeatures(LicenseRequestToQuantityFeatureFiltering licenseRequestToQuantityFeatureFiltering, SecurityContext securityContext) {
        return repository.listAllLicenseRequestToQuantityFeatures(licenseRequestToQuantityFeatureFiltering, securityContext);
    }

    public void validate(LicenseRequestToQuantityFeatureCreate licenseRequestToQuantityFeatureCreate, SecurityContext securityContext) {
        licenseRequestToFeatureService.validate(licenseRequestToQuantityFeatureCreate, securityContext);


    }

    public void validate(LicenseRequestToQuantityFeatureFiltering licenseRequestToQuantityFeatureFiltering, SecurityContext securityContext) {
        licenseRequestToFeatureService.validate(licenseRequestToQuantityFeatureFiltering, securityContext);
    }

    public PaginationResponse<LicenseRequestToQuantityFeature> getAllLicenseRequestToQuantityFeatures(LicenseRequestToQuantityFeatureFiltering licenseRequestToQuantityFeatureFiltering, SecurityContext securityContext) {
        List<LicenseRequestToQuantityFeature> list = listAllLicenseRequestToQuantityFeatures(licenseRequestToQuantityFeatureFiltering, securityContext);
        long count = repository.countAllLicenseRequestToQuantityFeatures(licenseRequestToQuantityFeatureFiltering, securityContext);
        return new PaginationResponse<>(list, licenseRequestToQuantityFeatureFiltering, count);
    }


}