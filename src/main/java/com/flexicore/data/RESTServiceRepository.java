/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.data;

import com.flexicore.annotations.InheritedComponent;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Named
@InheritedComponent
public class RESTServiceRepository {
	
	//TODO:change key to hold additional information about REST Service like description
	
	private HashMap<String, Function<HttpServletRequest,HttpServletResponse>> map= new HashMap<>();

	public RESTServiceRepository() {
		// TODO Auto-generated constructor stub
	}
	
	public boolean registerPluginRESTServices(HashMap<String, Function<HttpServletRequest,HttpServletResponse>> map){
		for (Entry<String, Function<HttpServletRequest,HttpServletResponse>> func : map.entrySet()) {
			if(map.containsKey(func.getKey())){
				return false;
			}
			
		}
		
		this.map.putAll(map);
		return true;
	}
	
	public Function<HttpServletRequest,HttpServletResponse> getService(String path){
		return map.get(path);
	}

}
