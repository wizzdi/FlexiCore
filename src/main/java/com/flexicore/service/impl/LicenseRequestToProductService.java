package com.flexicore.service.impl;


import com.atomikos.publish.EventPublisher;
import com.flexicore.data.LicenseRequestToProductRepository;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.interfaces.FlexiCoreService;
import com.flexicore.model.Baseclass;
import com.flexicore.model.licensing.LicenseRequestToProduct;
import com.flexicore.model.licensing.LicensingFeature;
import com.flexicore.model.licensing.LicensingProduct;
import com.flexicore.request.LicenseRequestToProductCreate;
import com.flexicore.request.LicenseRequestToProductFiltering;
import com.flexicore.request.LicenseRequestToProductUpdate;
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
public class LicenseRequestToProductService implements FlexiCoreService {


    @Autowired
    private LicenseRequestToProductRepository repository;

    @Autowired
    private LicenseRequestToEntityService licenseRequestToEntityService;

   private Logger logger = Logger.getLogger(getClass().getCanonicalName());


    public <T extends Baseclass> T getByIdOrNull(String id, Class<T> c, List<String> batchString, SecurityContext securityContext) {
        return repository.getByIdOrNull(id, c, batchString, securityContext);
    }


    public <T extends Baseclass> List<T> listByIds(Class<T> c, Set<String> ids, SecurityContext securityContext) {
        return repository.listByIds(c, ids, securityContext);
    }

    public LicenseRequestToProduct createLicenseRequestToProduct(LicenseRequestToProductCreate pluginCreationContainer, SecurityContext securityContext) {
        LicenseRequestToProduct licenseRequestToProduct = createLicenseRequestToProductNoMerge(pluginCreationContainer, securityContext);
        repository.merge(licenseRequestToProduct);
        licenseRequestUpdateEventEvent.publishEvent(new LicenseRequestUpdateEvent().setLicenseRequest(licenseRequestToProduct.getLicenseRequest()).setSecurityContext(securityContext));

        return licenseRequestToProduct;


    }

    public LicenseRequestToProduct createLicenseRequestToProductNoMerge(LicenseRequestToProductCreate licenseRequestToProductCreate, SecurityContext securityContext) {
        LicenseRequestToProduct licenseRequestToProduct = new LicenseRequestToProduct(licenseRequestToProductCreate.getName(), securityContext);
        updateLicenseRequestToProductNoMerge(licenseRequestToProduct, licenseRequestToProductCreate);
        return licenseRequestToProduct;
    }

    private boolean updateLicenseRequestToProductNoMerge(LicenseRequestToProduct licenseRequestToProduct, LicenseRequestToProductCreate licenseRequestToProductCreate) {
        boolean update = licenseRequestToEntityService.updateLicenseRequestToEntityNoMerge(licenseRequestToProduct, licenseRequestToProductCreate);
        if(licenseRequestToProductCreate.getLicensingProduct()!=null && (licenseRequestToProduct.getLicensingEntity()==null || !licenseRequestToProductCreate.getLicensingProduct().getId().equals(licenseRequestToProduct.getLicensingEntity().getId()))){
            licenseRequestToProduct.setLicensingEntity(licenseRequestToProductCreate.getLicensingProduct());
            update=true;
        }
        return update;
    }


    @Autowired
    private ApplicationEventPublisher licenseRequestUpdateEventEvent;


    public LicenseRequestToProduct updateLicenseRequestToProduct(LicenseRequestToProductUpdate licenseRequestToProductUpdate, SecurityContext securityContext) {
        LicenseRequestToProduct licenseRequestToProduct = licenseRequestToProductUpdate.getLicenseRequestToProduct();
        if (updateLicenseRequestToProductNoMerge(licenseRequestToProduct, licenseRequestToProductUpdate)) {
            repository.merge(licenseRequestToProduct);
            licenseRequestUpdateEventEvent.publishEvent(new LicenseRequestUpdateEvent().setLicenseRequest(licenseRequestToProduct.getLicenseRequest()).setSecurityContext(securityContext));

        }
        return licenseRequestToProduct;
    }

    public List<LicenseRequestToProduct> listAllLicenseRequestToProducts(LicenseRequestToProductFiltering licenseRequestToProductFiltering, SecurityContext securityContext) {
        return repository.listAllLicenseRequestToProducts(licenseRequestToProductFiltering, securityContext);
    }

    public void validate(LicenseRequestToProductCreate licenseRequestToProductCreate, SecurityContext securityContext) {
        licenseRequestToEntityService.validate(licenseRequestToProductCreate, securityContext);
        String licensingProductId=licenseRequestToProductCreate.getLicensingProductId();
        LicensingProduct licensingProduct=licensingProductId!=null?getByIdOrNull(licensingProductId, LicensingProduct.class,null,securityContext):null;
        if(licensingProduct==null && licensingProductId!=null){
            throw new BadRequestException("No License product with id "+licensingProductId);
        }
        licenseRequestToProductCreate.setLicensingProduct(licensingProduct);

    }

    public void validate(LicenseRequestToProductFiltering licenseRequestToProductFiltering, SecurityContext securityContext) {
        licenseRequestToEntityService.validate(licenseRequestToProductFiltering,securityContext);
    }

    public PaginationResponse<LicenseRequestToProduct> getAllLicenseRequestToProducts(LicenseRequestToProductFiltering licenseRequestToProductFiltering, SecurityContext securityContext) {
        List<LicenseRequestToProduct> list = listAllLicenseRequestToProducts(licenseRequestToProductFiltering, securityContext);
        long count = repository.countAllLicenseRequestToProducts(licenseRequestToProductFiltering, securityContext);
        return new PaginationResponse<>(list, licenseRequestToProductFiltering, count);
    }


}