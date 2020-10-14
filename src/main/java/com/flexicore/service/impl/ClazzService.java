/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.service.impl;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import javax.inject.Named;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.interfaces.FlexiCoreService;
import com.flexicore.model.*;
import com.flexicore.request.ClazzFilter;
import com.flexicore.security.SecurityContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Named
@Primary
@Component
public class ClazzService implements FlexiCoreService{
	private Logger logger = Logger.getLogger(getClass().getCanonicalName());


	public List<Clazz> getallClazz(User user) {
		// TODO: securiy stuff with user
		return Baseclass.getAllClazz();
	}

	public ClazzService() {
		// TODO Auto-generated constructor stub
	}

	public Clazz getclazz(String clazzName) {
		return Baseclass.getClazzByName(clazzName);
	}
	
	  public static List<Field> getInheritedFields(Class<?> type) {
	        List<Field> fields = new ArrayList<>();
	        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
	            fields.addAll(Arrays.asList(c.getDeclaredFields()));
	        }
	        return fields;
	    }


	public PaginationResponse<Clazz> getAllClazz(ClazzFilter filter, SecurityContext securityContext) {
		List<Clazz> allClazz = Baseclass.getAllClazz();
		Stream<Clazz> clazzStream = allClazz.parallelStream();

		if(filter.getNameLike()!=null){
			String nameLike=filter.getNameLike().replace("%","");
			clazzStream=clazzStream.filter(f->f.getName()!=null && f.getName().contains(nameLike));
		}
		if(filter.getPageSize()!=null && filter.getCurrentPage()!=null && filter.getCurrentPage()> -1 && filter.getPageSize() > 0){
			clazzStream=clazzStream.skip(filter.getPageSize()*filter.getCurrentPage()).limit(filter.getPageSize());
		}
		return new PaginationResponse<>(clazzStream.collect(Collectors.toList()),filter,allClazz.size());
	}
}
