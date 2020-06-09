package com.flexicore.request;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.flexicore.data.jsoncontainers.CrossLoaderResolver;
import com.flexicore.model.licensing.LicenseRequestToEntity;

import java.time.OffsetDateTime;

/**
 * Created by Asaf on 18/10/2016.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "type")
@JsonTypeIdResolver(CrossLoaderResolver.class)
public class RequestToLicenseEntityHolder {

    private OffsetDateTime granted;
    private OffsetDateTime expiration;

    private boolean perpetual;
    private boolean demo;
    private String canonicalName;




    public RequestToLicenseEntityHolder() {
    }

    public RequestToLicenseEntityHolder(LicenseRequestToEntity licenseRequestToEntity) {
        this.granted=licenseRequestToEntity.getGranted();
        this.expiration=licenseRequestToEntity.getExpiration();
        this.perpetual=licenseRequestToEntity.isPerpetual();
        this.demo=licenseRequestToEntity.isDemo();
        this.canonicalName=licenseRequestToEntity.getLicensingEntity().getCanonicalName();

    }


    public OffsetDateTime getGranted() {
        return granted;
    }

    public <T extends RequestToLicenseEntityHolder> T setGranted(OffsetDateTime granted) {
        this.granted = granted;
        return (T) this;
    }

    public OffsetDateTime getExpiration() {
        return expiration;
    }

    public <T extends RequestToLicenseEntityHolder> T setExpiration(OffsetDateTime expiration) {
        this.expiration = expiration;
        return (T) this;
    }

    public boolean isPerpetual() {
        return perpetual;
    }

    public <T extends RequestToLicenseEntityHolder> T setPerpetual(boolean perpetual) {
        this.perpetual = perpetual;
        return (T) this;
    }

    public boolean isDemo() {
        return demo;
    }

    public <T extends RequestToLicenseEntityHolder> T setDemo(boolean demo) {
        this.demo = demo;
        return (T) this;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public <T extends RequestToLicenseEntityHolder> T setCanonicalName(String canonicalName) {
        this.canonicalName = canonicalName;
        return (T) this;
    }

}
