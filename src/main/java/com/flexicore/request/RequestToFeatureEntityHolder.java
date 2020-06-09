package com.flexicore.request;

import com.flexicore.model.licensing.LicenseRequestToEntity;

import java.time.OffsetDateTime;

/**
 * Created by Asaf on 18/10/2016.
 */
public class RequestToFeatureEntityHolder extends RequestToLicenseEntityHolder{


    public RequestToFeatureEntityHolder() {
    }

    public RequestToFeatureEntityHolder(LicenseRequestToEntity licenseRequestToEntity) {
        super(licenseRequestToEntity);
    }
}
