package com.flexicore.data;

import com.flexicore.annotations.InheritedComponent;

import java.util.concurrent.ConcurrentHashMap;

@InheritedComponent
public class TestsRepository {
	
	private static ConcurrentHashMap<String,Class<?>> tests= new ConcurrentHashMap<>();

	public static Class<?> get(Object key) {
		return tests.get(key);
	}

	public static Class<?> put(String key, Class<?> value) {
		return tests.put(key, value);
	}
	
	

}
