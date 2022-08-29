package com.kmarinos.sqlutils.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SelectResult {

	private final Map<Key<?>, Object> values = new HashMap<>();
	private final Map<String, Key<?>> keys = new HashMap<>();

	@SuppressWarnings("unused")
	private <T> void put(Key<T> key, T value) {
		values.put(key, value);
	}

	public <T> void put(String identifier, Class<T> type, Object value) {
		Key<T> key = new SelectResult.Key<T>(identifier, type);
		keys.put(identifier, key);
		values.put(key, value);
	}

	private <T> T get(Key<T> key) {
		if(key==null) {
			return null;
		}
		return key.type.cast(values.get(key));
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String keyName) {
		Key<?> key = keys.get(keyName);
		if(key==null) {
			
		}
		return (T)  get(key);
	}
	public <T> T get(String keyName,Class<T>type) {
		return get(keyName);
	}
	public Map<String,?> getAll(){
		if(keys==null) {
			return new HashMap<>();
		}
		Map<String,?> map = new HashMap<>();
		for(String strKey:keys.keySet()) {
			map.put(strKey, get(strKey));
		}
		return map;
//		return keys.keySet().stream().filter(x->x!=null).collect(Collectors.toMap(x->x, x->get(x)));
	}
	public Class<?> typeOf(String keyName){
		return keys.get(keyName).type;
	}

	public class Key<T> {

		final String identifier;
		final Class<T> type;

		public Key(String identifier, Class<T> type) {
			this.identifier = identifier;
			this.type = type;
		}
	}
}
