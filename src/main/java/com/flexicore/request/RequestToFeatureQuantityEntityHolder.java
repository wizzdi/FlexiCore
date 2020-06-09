package com.flexicore.request;

import com.flexicore.model.licensing.LicenseRequestToQuantityFeature;

/**
 * Created by Asaf on 18/10/2016.
 */
public class RequestToFeatureQuantityEntityHolder extends RequestToFeatureEntityHolder{

    private int quantityLimit;

    public RequestToFeatureQuantityEntityHolder() {
    }

    public RequestToFeatureQuantityEntityHolder(LicenseRequestToQuantityFeature licenseRequestToEntity) {
        super(licenseRequestToEntity);
        this.quantityLimit=licenseRequestToEntity.getQuantityLimit();
    }

    public int getQuantityLimit() {
        return quantityLimit;
    }

    public <T extends RequestToFeatureQuantityEntityHolder> T setQuantityLimit(int quantityLimit) {
        this.quantityLimit = quantityLimit;
        return (T) this;
    }
}
