package com.flexicore.service.impl;


import com.flexicore.data.LicenseRequestToEntityRepository;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.interfaces.FlexiCoreService;
import com.flexicore.model.Baseclass;
import com.flexicore.model.licensing.LicenseRequest;
import com.flexicore.model.licensing.LicenseRequestToEntity;
import com.flexicore.request.LicenseRequestToEntityCreate;
import com.flexicore.request.LicenseRequestToEntityFiltering;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.BaseclassNewService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Autowired;
import javax.ws.rs.BadRequestException;
import java.time.ZoneId;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@Primary
@Component
public class LicenseRequestToEntityService implements FlexiCoreService {


    @Autowired
    private LicenseRequestToEntityRepository repository;

    @Autowired
    private BaseclassNewService baseclassNewService;

   private Logger logger = Logger.getLogger(getClass().getCanonicalName());


    public <T extends Baseclass> T getByIdOrNull(String id, Class<T> c, List<String> batchString, SecurityContext securityContext) {
        return repository.getByIdOrNull(id, c, batchString, securityContext);
    }


    public <T extends Baseclass> List<T> listByIds(Class<T> c, Set<String> ids, SecurityContext securityContext) {
        return repository.listByIds(c, ids, securityContext);
    }

    public LicenseRequestToEntity createLicenseRequestToEntity(LicenseRequestToEntityCreate pluginCreationContainer, SecurityContext securityContext) {
        LicenseRequestToEntity licenseRequestToEntity = createLicenseRequestToEntityNoMerge(pluginCreationContainer, securityContext);
        repository.merge(licenseRequestToEntity);
        return licenseRequestToEntity;


    }

    public LicenseRequestToEntity createLicenseRequestToEntityNoMerge(LicenseRequestToEntityCreate licenseRequestToEntityCreate, SecurityContext securityContext) {
        LicenseRequestToEntity licenseRequestToEntity = new LicenseRequestToEntity(licenseRequestToEntityCreate.getName(), securityContext);
        updateLicenseRequestToEntityNoMerge(licenseRequestToEntity, licenseRequestToEntityCreate);
        return licenseRequestToEntity;
    }

    public boolean updateLicenseRequestToEntityNoMerge(LicenseRequestToEntity licenseRequestToEntity, LicenseRequestToEntityCreate licenseRequestToEntityCreate) {
        boolean update = baseclassNewService.updateBaseclassNoMerge(licenseRequestToEntityCreate, licenseRequestToEntity);
        if(licenseRequestToEntityCreate.getDemo()!=null && !licenseRequestToEntityCreate.getDemo().equals(licenseRequestToEntity.isDemo())){
            licenseRequestToEntity.setDemo(licenseRequestToEntityCreate.getDemo());
            update=true;
        }
        if(licenseRequestToEntityCreate.getGranted()!=null && !licenseRequestToEntityCreate.getGranted().withZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime().equals(licenseRequestToEntity.getGranted())){
            licenseRequestToEntity.setGranted(licenseRequestToEntityCreate.getGranted().withZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime());
            update=true;
        }

        if(licenseRequestToEntityCreate.getExpiration()!=null && !licenseRequestToEntityCreate.getExpiration().withZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime().equals(licenseRequestToEntity.getExpiration())){
            licenseRequestToEntity.setExpiration(licenseRequestToEntityCreate.getExpiration().withZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime());
            update=true;
        }
        if(licenseRequestToEntityCreate.getPerpetual()!=null && !licenseRequestToEntityCreate.getPerpetual().equals(licenseRequestToEntity.isPerpetual())){
            licenseRequestToEntity.setPerpetual(licenseRequestToEntityCreate.getPerpetual());
            update=true;
        }
        if(licenseRequestToEntityCreate.getLicenseRequest()!=null && (licenseRequestToEntity.getLicenseRequest()==null || !licenseRequestToEntityCreate.getLicenseRequest().getId().equals(licenseRequestToEntity.getLicenseRequest().getId()))){
            licenseRequestToEntity.setLicenseRequest(licenseRequestToEntityCreate.getLicenseRequest());
            update=true;
        }

        return update;
    }
    

    public List<LicenseRequestToEntity> listAllLicenseRequestToEntities(LicenseRequestToEntityFiltering licenseRequestToEntityFiltering, SecurityContext securityContext) {
        return repository.listAllLicenseRequestToEntities(licenseRequestToEntityFiltering, securityContext);
    }

    public void validate(LicenseRequestToEntityCreate licenseRequestToEntityCreate, SecurityContext securityContext) {
        baseclassNewService.validate(licenseRequestToEntityCreate, securityContext);
        String licenseRequestId=licenseRequestToEntityCreate.getLicenseRequestId();
        LicenseRequest licenseRequest=licenseRequestId!=null?getByIdOrNull(licenseRequestId,LicenseRequest.class,null,securityContext):null;
        if(licenseRequest==null && licenseRequestId!=null){
            throw new BadRequestException("No License request with id "+licenseRequestId);
        }
        licenseRequestToEntityCreate.setLicenseRequest(licenseRequest);

    }

    public void validateCreate(LicenseRequestToEntityCreate licenseRequestToEntityCreate, SecurityContext securityContext) {
        validate(licenseRequestToEntityCreate,securityContext);
        if((licenseRequestToEntityCreate.getPerpetual()==null || !licenseRequestToEntityCreate.getPerpetual()) && licenseRequestToEntityCreate.getExpiration()==null){
            throw new BadRequestException("Perpetual or expiration date should be set");
        }
    }

    public void validate(LicenseRequestToEntityFiltering licenseRequestToEntityFiltering, SecurityContext securityContext) {
        baseclassNewService.validateFilter(licenseRequestToEntityFiltering,securityContext);
        Set<String> licenseRequestIds=licenseRequestToEntityFiltering.getLicenseRequestIds();
        Map<String,LicenseRequest> licenseRequestMap=licenseRequestIds.isEmpty()?new HashMap<>():listByIds(LicenseRequest.class,licenseRequestIds,securityContext).parallelStream().collect(Collectors.toMap(f->f.getId(),f->f));
        licenseRequestIds.removeAll(licenseRequestMap.keySet());
        if(!licenseRequestIds.isEmpty()){
            throw new BadRequestException("No License Requests with ids "+licenseRequestIds);
        }
        licenseRequestToEntityFiltering.setLicenseRequests(new ArrayList<>(licenseRequestMap.values()));
    }

    public PaginationResponse<LicenseRequestToEntity> getAllLicenseRequestToEntities(LicenseRequestToEntityFiltering licenseRequestToEntityFiltering, SecurityContext securityContext) {
        List<LicenseRequestToEntity> list = listAllLicenseRequestToEntities(licenseRequestToEntityFiltering, securityContext);
        long count = repository.countAllLicenseRequestToEntities(licenseRequestToEntityFiltering, securityContext);
        return new PaginationResponse<>(list, licenseRequestToEntityFiltering, count);
    }


}