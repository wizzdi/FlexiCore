package com.flexicore.service.impl;

import com.flexicore.annotations.plugins.PluginInfo;
import com.flexicore.exceptions.ExceededQuota;
import com.flexicore.interfaces.FlexiCoreService;
import com.flexicore.model.Baseclass;
import com.flexicore.model.Clazz;
import com.flexicore.model.ClazzIdFiltering;
import com.flexicore.model.Tenant;
import com.flexicore.model.licensing.LicenseRequest;
import com.flexicore.model.licensing.LicenseRequestToQuantityFeature;
import com.flexicore.request.BaseclassCountRequest;
import com.flexicore.request.LicenseRequestFiltering;
import com.flexicore.request.LicenseRequestToQuantityFeatureFiltering;
import com.flexicore.request.UpdateLicensingCache;
import com.flexicore.response.BaseclassCount;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import javax.enterprise.event.ObservesAsync;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@Primary
@Component
public class LicenseEnforcer implements FlexiCoreService {

    private static AtomicBoolean init = new AtomicBoolean(false);

    private static Cache<String, Integer> requestedLicense = CacheBuilder.newBuilder().build();
    private static Cache<String, LicenseRequest> licensed = CacheBuilder.newBuilder().build();
    private static Cache<String, AtomicLong> cachedCount=CacheBuilder.newBuilder().build();


   private Logger logger = Logger.getLogger(getClass().getCanonicalName());

    @Autowired
    private LicenseRequestToQuantityFeatureService licenseRequestToQuantityFeatureService;

    @Autowired
    private LicenseRequestService licenseRequestService;
    @Autowired
    private BaseclassService baseclassService;


    @PostConstruct
    public void onStart() {
        logger.info("License Enforcer Started");
        if (init.compareAndSet(false, true)) {
            //updateLicensingCache(new UpdateLicensingCache());
            Baseclass.setOnBaseclassCreated(b -> {
                if(b.getTenant()!=null){
                    String canonicalName = b.getClass().getCanonicalName();
                    Tenant tenant = b.getTenant();
                    String tenantId = tenant.getId();
                    String key=getKey(tenantId, canonicalName);
                    Integer quantity=requestedLicense.getIfPresent(key);
                    if(quantity!=null){
                        try {
                            AtomicLong currentQuantity=cachedCount.get(key,()->new AtomicLong(0));
                            long val=currentQuantity.updateAndGet(operand -> operand>quantity?operand:operand+1);
                            LicenseRequest license=licensed.getIfPresent(key);
                            if(license==null){
                                throw new ExceededQuota("Quota of "+canonicalName+" for tenant +"+tenant.getName()+"("+tenantId+") Exceeded - no license");
                            }
                            if( val > quantity){
                                throw new ExceededQuota("Quota of "+canonicalName+" for tenant +"+tenant.getName()+"("+tenantId+") Exceeded , max is "+quantity +" actual is "+val);
                            }
                        } catch (ExecutionException e) {
                            logger.log(Level.SEVERE,"failed checking current quantity",e);
                        }
                    }
                }

            });
        }
    }



    public void updateLicensingCache(@ObservesAsync UpdateLicensingCache updateLicensingCache) {
        List<LicenseRequest> licenseRequests =licenseRequestService.listAllLicenseRequests(new LicenseRequestFiltering(),null);
        List<LicenseRequestToQuantityFeature> licenseRequestToQuantityFeatures = licenseRequests.isEmpty()?new ArrayList<>():licenseRequestToQuantityFeatureService.listAllLicenseRequestToQuantityFeatures(new LicenseRequestToQuantityFeatureFiltering().setLicenseRequests(licenseRequests), null);
        Map<String,List<LicenseRequestToQuantityFeature>> links = licenseRequestToQuantityFeatures.parallelStream().filter(f->f.getLicenseRequest()!=null).collect(Collectors.groupingBy(f->f.getLicenseRequest().getId()));

        Set<String> clazzNames=new HashSet<>();
        for (Map.Entry<String, List<LicenseRequestToQuantityFeature>> stringListEntry : links.entrySet()) {
            Boolean validated=null;
            for (LicenseRequestToQuantityFeature licenseRequestToQuantityFeature : stringListEntry.getValue()) {
                if(validated==null){
                    validated=licenseRequestService.isLicenseValid(licenseRequestToQuantityFeature.getLicenseRequest());
                }
                clazzNames.add(licenseRequestToQuantityFeature.getLicensingEntity().getCanonicalName());
                String key = getKey(licenseRequestToQuantityFeature);
                requestedLicense.put(key,licenseRequestToQuantityFeature.getQuantityLimit());
                if(validated){
                    licensed.put(key,licenseRequestToQuantityFeature.getLicenseRequest());
                }
            }
        }
        List<ClazzIdFiltering> clazzIdFilterings=clazzNames.parallelStream().map(f->Baseclass.getClazzbyname(f)).filter(f->f!=null).map(f->new ClazzIdFiltering().setId(f.getId())).collect(Collectors.toList());
        List<BaseclassCount> baseclassCounts=clazzIdFilterings.isEmpty()?new ArrayList<>():baseclassService.getBaseclassCount(new BaseclassCountRequest().setGroupByTenant(true).setClazzIds(clazzIdFilterings),null);
        for (BaseclassCount baseclassCount : baseclassCounts) {
            cachedCount.put(getKey(baseclassCount.getTenant().getId(),baseclassCount.getCanonicalName()),new AtomicLong(baseclassCount.getCount()));
        }
    }

    private String getKey(LicenseRequestToQuantityFeature licenseRequestToQuantityFeature) {
        return getKey(licenseRequestToQuantityFeature.getLicenseRequest().getLicensedTenant().getId(),licenseRequestToQuantityFeature.getLicensingEntity().getCanonicalName());
    }

    private String getKey(String tenantId, String canonicalName) {
        return tenantId+":"+canonicalName;
    }

}
