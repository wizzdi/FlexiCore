package com.flexicore.service.impl;

import com.flexicore.model.licensing.LicenseRequest;
import com.flexicore.security.SecurityContext;

public class LicenseRequestUpdateEvent {
    private LicenseRequest licenseRequest;
    private SecurityContext securityContext;

    public LicenseRequest getLicenseRequest() {
        return licenseRequest;
    }

    public <T extends LicenseRequestUpdateEvent> T setLicenseRequest(LicenseRequest licenseRequest) {
        this.licenseRequest = licenseRequest;
        return (T) this;
    }

    public SecurityContext getSecurityContext() {
        return securityContext;
    }

    public <T extends LicenseRequestUpdateEvent> T setSecurityContext(SecurityContext securityContext) {
        this.securityContext = securityContext;
        return (T) this;
    }
}
