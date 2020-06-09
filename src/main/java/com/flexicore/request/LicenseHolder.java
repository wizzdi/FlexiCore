package com.flexicore.request;


import com.flexicore.model.licensing.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Asaf on 17/10/2016.
 */
public class LicenseHolder {

    private String tenantId;
    private String tenantName;
    private String macAddress;
    private String diskSerialNumber;
    private String externalHWSerialNumber;
    private List<RequestToLicenseEntityHolder> entities = new ArrayList<>();

    public LicenseHolder(LicenseRequest other) {
        if(other.getLicensedTenant()!=null){
            this.tenantId=other.getLicensedTenant().getId();
            this.tenantName=other.getLicensedTenant().getName();
        }

        this.macAddress = other.getMacAddress();
        this.diskSerialNumber = other.getDiskSerialNumber();
        this.externalHWSerialNumber = other.getExternalHWSerialNumber();
    }

    public LicenseHolder(LicenseRequest licenseRequest, List<LicenseRequestToEntity> entities) {
        this(licenseRequest);
        this.entities = entities.parallelStream().map(f->getHolder(f)).filter(f->f!=null).sorted(Comparator.comparing(RequestToLicenseEntityHolder::getCanonicalName)).collect(Collectors.toList());
    }

    private RequestToLicenseEntityHolder getHolder(LicenseRequestToEntity licenseRequestToEntity) {
        if(licenseRequestToEntity instanceof LicenseRequestToQuantityFeature){
            return new RequestToFeatureQuantityEntityHolder((LicenseRequestToQuantityFeature) licenseRequestToEntity);
        }
        else{
            if(licenseRequestToEntity instanceof LicenseRequestToFeature){
                return new RequestToFeatureEntityHolder(licenseRequestToEntity);
            }
            else{
                if(licenseRequestToEntity instanceof LicenseRequestToProduct){
                    return new RequestToProductEntityHolder(licenseRequestToEntity);
                }
            }
        }
        return null;

    }


    public LicenseHolder() {
    }

    public List<RequestToLicenseEntityHolder> getEntities() {
        return entities;
    }

    public <T extends LicenseHolder> T setEntities(List<RequestToLicenseEntityHolder> entities) {
        this.entities = entities;
        return (T) this;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public <T extends LicenseHolder> T setMacAddress(String macAddress) {
        this.macAddress = macAddress;
        return (T) this;
    }

    public String getDiskSerialNumber() {
        return diskSerialNumber;
    }

    public <T extends LicenseHolder> T setDiskSerialNumber(String diskSerialNumber) {
        this.diskSerialNumber = diskSerialNumber;
        return (T) this;
    }

    public String getExternalHWSerialNumber() {
        return externalHWSerialNumber;
    }

    public <T extends LicenseHolder> T setExternalHWSerialNumber(String externalHWSerialNumber) {
        this.externalHWSerialNumber = externalHWSerialNumber;
        return (T) this;
    }

    public String getTenantId() {
        return tenantId;
    }

    public <T extends LicenseHolder> T setTenantId(String tenantId) {
        this.tenantId = tenantId;
        return (T) this;
    }

    public String getTenantName() {
        return tenantName;
    }

    public <T extends LicenseHolder> T setTenantName(String tenantName) {
        this.tenantName = tenantName;
        return (T) this;
    }
}
