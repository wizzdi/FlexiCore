package com.flexicore.health;

import com.flexicore.model.ModuleManifest;
import com.flexicore.service.impl.PluginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.stream.Collectors;


@Primary
@Component
public class PluginsHealthCheck implements HealthIndicator {

    @Autowired
    private PluginService pluginService;


    @Override
    public Health health() {
        Health.Builder responseBuilder = Health.up();
        Map<String, ModuleManifest> map= pluginService.getAll().parallelStream().collect(Collectors.toMap(f->f.getModuleManifest().getUuid(),f->f.getModuleManifest(),(a,b)->a));
        for (ModuleManifest moduleManifest : map.values()) {
            responseBuilder.withDetail(moduleManifest.getUuid()+"("+moduleManifest.getPluginType()+")",moduleManifest.getVersion());

        }


        return responseBuilder.build();

    }

}
