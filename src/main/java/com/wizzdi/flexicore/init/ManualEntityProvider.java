package com.wizzdi.flexicore.init;

import com.flexicore.converters.JsonConverter;
import com.flexicore.model.Baseclass;
import com.flexicore.model.User;
import com.flexicore.model.security.SecurityPolicy;
import com.wizzdi.flexicore.boot.dynamic.invokers.model.DynamicExecution;
import com.wizzdi.flexicore.boot.jpa.service.EntitiesHolder;
import com.wizzdi.flexicore.file.model.FileResource;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.Arrays;
import java.util.HashSet;

@Configuration
public class ManualEntityProvider {

	@Bean
	@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)

	public EntitiesHolder manualEntityHolder(){
		return new EntitiesHolder(new HashSet<>(Arrays.asList(FileResource.class,Baseclass.class, DynamicExecution.class, User.class, JsonConverter.class, SecurityPolicy.class)));
	}
}
