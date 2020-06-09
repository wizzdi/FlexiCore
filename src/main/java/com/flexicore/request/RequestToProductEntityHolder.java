package com.flexicore.request;

import com.flexicore.model.licensing.LicenseRequestToEntity;

/**
 * Created by Asaf on 18/10/2016.
 */
public class RequestToProductEntityHolder extends RequestToLicenseEntityHolder{


    public RequestToProductEntityHolder() {
    }

    public RequestToProductEntityHolder(LicenseRequestToEntity licenseRequestToEntity) {
        super(licenseRequestToEntity);
    }
}
