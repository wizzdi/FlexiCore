package com.flexicore.service.impl;

import com.flexicore.interfaces.FlexiCoreService;
import com.flexicore.response.HealthStatusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Primary
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class HealthService implements com.flexicore.service.HealthService {

	@Autowired
	private HealthEndpoint healthEndpoint;
	private HealthComponent healthComponent;

	@Value("${flexicore.health.minInterval:30000}")
	private long minInterval;
	private static long time;


	@Override
	public HealthComponent getHealth() {
		if(healthComponent==null||System.currentTimeMillis() - time >minInterval ){
			healthComponent= healthEndpoint.health();
			time=System.currentTimeMillis();
		}
		return healthComponent;
	}

	@Override
	public HealthStatusResponse healthCheck() {
		return new HealthStatusResponse(getHealth());
	}

}
