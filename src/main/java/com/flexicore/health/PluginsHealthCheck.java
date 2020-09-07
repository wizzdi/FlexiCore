package com.flexicore.health;

import com.flexicore.data.jsoncontainers.PluginType;
import com.flexicore.model.ModuleManifest;
import com.flexicore.service.impl.PluginService;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
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

    @Autowired
    private PluginManager pluginManager;

    @PostConstruct
    private void init() {
        logger.info("plugins health check post construct called");
    }

    @Override
    public Health health() {
        Health.Builder responseBuilder = Health.up();

        if(pluginService!=null){
            for (PluginWrapper pluginWrapper : pluginManager.getStartedPlugins()) {
                String version = pluginWrapper.getDescriptor()!=null?pluginWrapper.getDescriptor().getVersion():"unknown";
                responseBuilder.withDetail(pluginWrapper.getPluginId() + "(" + PluginType.Service.name() + ")", version);

            }

        }


        return responseBuilder.build();

    }

}
