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

import com.flexicore.annotations.FieldForView;
import com.flexicore.data.jsoncontainers.ClazzLinkContainer;
import com.flexicore.data.jsoncontainers.FieldContainer;
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
	private ConcurrentHashMap<String, List<ClazzLinkContainer>> connectionsCache= new ConcurrentHashMap<>();


	public List<Clazz> getallClazz(User user) {
		// TODO: securiy stuff with user
		return Baseclass.getAllClazz();
	}

	public ClazzService() {
		// TODO Auto-generated constructor stub
	}

	public List<FieldContainer> getFields(Clazz clazz,SecurityContext securityContext) {
		List<FieldContainer> fields = new ArrayList<>();
		try {
			String name = clazz.getName();

			Class<?> c = Class.forName(name);

			List<Field> f = getInheritedFields(c);
			for (Field field : f) {
				if (field.isAnnotationPresent(FieldForView.class)&&!field.isAnnotationPresent(OneToMany.class)&&!field.isAnnotationPresent(OneToOne.class)) {
					FieldForView v = field.getAnnotation(FieldForView.class);
					fields.add(new FieldContainer(field.getName(), field.getType().getCanonicalName(), v.validation(),
							v.group(),v.required()));
				}
			}

		} catch (ClassNotFoundException e) {
			logger.log(Level.SEVERE, "clazz name" + clazz.getName() + "is not canonicalClassName", e);
		}
		return fields;
	}

	public Clazz getclazz(String clazzName) {
		return Baseclass.getClazzbyname(clazzName);
	}



	@SuppressWarnings("unused")
	public List<ClazzLinkContainer> getAssociations(Clazz clazz, SecurityContext securityContext) {
		List<ClazzLinkContainer> toReturn=connectionsCache.get(clazz.getName());
		try {
			Class<?> c = Class.forName(clazz.getName());
		if(toReturn==null){
			toReturn= new ArrayList<>();
			
			
			
			List<Field> f = getInheritedFields(c);
			
			for (Field field : f) {
				if (field.isAnnotationPresent(OneToMany.class)) {
				
					Type type=field.getGenericType();
					if(type instanceof ParameterizedType){
						ParameterizedType ptype=(ParameterizedType) type;
						Class<?> bLinkType=Class.forName(ptype.getActualTypeArguments()[0].getTypeName());
						if(Baselink.class.isAssignableFrom(bLinkType)){
							OneToMany oTm=field.getAnnotation(OneToMany.class);
							boolean isOthersideRight=(!oTm.mappedBy().equalsIgnoreCase("rightside"));
							logger.info("adding clazzLink: "+bLinkType.getCanonicalName());
							toReturn.add(new ClazzLinkContainer((ClazzLink) Baseclass.getClazzbyname(bLinkType.getCanonicalName()),isOthersideRight));
						}
					}
					
				}
			}
		}
		} catch (ClassNotFoundException e) {
			logger.log(Level.SEVERE, "Class: " + clazz.getName() +" not found", e);
		}
		return toReturn;
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
