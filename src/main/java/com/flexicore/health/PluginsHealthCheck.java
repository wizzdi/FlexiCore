package com.flexicore.health;

import com.flexicore.model.ModuleManifest;
import com.flexicore.service.impl.PluginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.stream.Collectors;


@Primary
@Component
public class PluginsHealthCheck implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(PluginsHealthCheck.class);
    @Autowired
    private PluginService pluginService;

    @PostConstruct
    private void init() {
        logger.info("plugins health check post construct called");
    }

    @Override
    public Health health() {
        Health.Builder responseBuilder = Health.up();
        if(pluginService!=null){
            Map<String, ModuleManifest> map = pluginService.getAll().parallelStream().filter(f -> f != null && f.getModuleManifest() != null && f.getModuleManifest().getUuid() != null).collect(Collectors.toMap(f -> f.getModuleManifest().getUuid(), f -> f.getModuleManifest(), (a, b) -> a));
            for (ModuleManifest moduleManifest : map.values()) {
                responseBuilder.withDetail(moduleManifest.getUuid() + "(" + moduleManifest.getPluginType() + ")", moduleManifest.getVersion());

            }

        }


        return responseBuilder.build();

    }

}
